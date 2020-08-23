package info.repy.m3u8java.core;

import java.net.*;
import java.security.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.*;
import javax.crypto.spec.*;

public class M3U8 extends AbstractExecuter {

	private static enum KeyType {
		NONE,
		AES128
	}

	private static class Download {

		private final double time;
		private final URL url;
		private final KeyType type;
		private final byte[] key;
		private final byte[] iv;

		public Download(double time, URL url, KeyType type, byte[] key, byte[] iv) {
			this.time = time;
			this.url = url;
			this.type = type;
			this.key = key.clone();
			this.iv = iv.clone();
		}

		public double getTime() {
			return time;
		}

		public URL getUrl() {
			return url;
		}

		public KeyType getType() {
			return type;
		}

		public byte[] getKey() {
			return key;
		}

		public byte[] getIv() {
			return iv;
		}
	}

	public M3U8(String url, String savefilename) {
		super(url, savefilename);
	}

	public M3U8(String url, OutputStream stream){
		super(url, stream);
	}

	@Override
	protected void run(AsyncListener listener) {
		if (status != Status.NONE) return;
		status = Status.RUNNING;
		try {
			URL mediaURL = null;
			URL m3u8Url = new URL(this.url);
			try (InputStream is = m3u8Url.openStream()) {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
					String line;
					ArrayList<Map.Entry<Long, URL>> urls = new ArrayList<>();
					long newurl = 0;
					while ((line = br.readLine()) != null) {
						line = line.trim();
						Property property = checkProperty(line);
						if (property == null) {
							newurl = 0;
							System.out.print("not support : ");
							System.out.println(line);
						} else if (property.type.equals("EXT-X-STREAM-INF")) {
							if (property.properties.get("BANDWIDTH") != null) newurl = Long.parseLong(property.properties.get("BANDWIDTH"));
							else newurl = 1;
						} else if (property.type.equals("FILE") && newurl > 0) {
							urls.add(new java.util.AbstractMap.SimpleEntry<>(newurl, new URL(m3u8Url, line)));
							newurl = 0;
						} else {
							newurl = 0;
							System.out.print("not support : ");
							System.out.println(line);
						}
					}
					urls.sort(new Comparator<Map.Entry<Long, URL>>() {
						@Override
						public int compare(Map.Entry<Long, URL> o1, Map.Entry<Long, URL> o2) {
							return -Long.compare(o1.getKey(), o2.getKey());
						}
					});
					if (urls.size() > 0) {
						m3u8Url = urls.get(0).getValue();
					}
				}
			}
			if (mediaURL == null) mediaURL = m3u8Url;
			ArrayList<Download> list = new ArrayList<>();
			double time = 0.0;

			try (InputStream is = mediaURL.openStream()) {
				try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
					KeyType type = KeyType.NONE;
					byte[] key = new byte[16];
					byte[] iv = new byte[16];
					String line;
					while ((line = br.readLine()) != null) {
						line = line.trim();
						Property property = checkProperty(line);
						if (property == null) {
							System.out.print("not support : ");
							System.out.println(line);
						} else if (property.type.equals("EXTINF")) {
							time = time + Double.parseDouble(property.values[0]);
						} else if (property.type.equals("EXT-X-KEY")) {
							type = KeyType.AES128;
							URL keyUrl = new URL(mediaURL, property.properties.get("URI"));
							try (InputStream ks = keyUrl.openStream()) {
								ks.read(key);
							}
							if (property.properties.get("IV") != null) {
								String ivstr = property.properties.get("IV");
								ivstr = ivstr.substring(2);
								for (int i = 0; i < iv.length; i++) {
									iv[i] = (byte) Integer.parseInt(ivstr.substring(i * 2, (i + 1) * 2), 16);
								}
							} else {
								for (int i = 0; i < iv.length; i++) {
									iv[i] = 0;
								}
							}
						} else if (property.type.equals("FILE")) {
							URL tsUrl = new URL(mediaURL, line);
							list.add(new Download(time, tsUrl, type, key, iv));
							for (int i = iv.length; i > 0; i--) {
								iv[i - 1] = (byte) (iv[i - 1] + 1);
								if (iv[i - 1] != 0) break;
							}
						} else {
							System.err.print("not support : ");
							System.err.println(line);
						}
					}
				}
			}

			listener.progress(0.0, time);

			try (OutputStream fileStream = this.output;) {
				for (Download down : list) {
					if (status != Status.RUNNING) return;
					Cipher cipher;
					KeyType type = down.getType();
					if (type == KeyType.AES128) {
						cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
						Key skey = new SecretKeySpec(down.getKey(), "AES");
						IvParameterSpec param = new IvParameterSpec(down.getIv());
						cipher.init(Cipher.DECRYPT_MODE, skey, param);
					} else if (type == KeyType.NONE) {
						cipher = new NullCipher();
					} else {
						throw new IllegalStateException();
					}
					try (InputStream ts = down.getUrl().openStream(); BufferedInputStream bufferedInputStream = new BufferedInputStream(ts); CipherInputStream cipherInputStream = new CipherInputStream(bufferedInputStream, cipher)) {
						boolean firstPacket = true;
						boolean endByte = false;
						int PACKETSIZE = 188;
						byte[] buf = new byte[PACKETSIZE]; // mpegtsが188でワンパケットのため188
						int readByte;
						int nowByte;
						while (!endByte) {
							nowByte = 0;
							readByte = 0;
							while (nowByte < PACKETSIZE && (readByte = cipherInputStream.read(buf, nowByte, PACKETSIZE - nowByte)) >= 0) {
								nowByte += readByte;
							}
							if (readByte < 0) {
								endByte = true;
							}

							if (firstPacket && buf[0] != 0x47) { // ワンパケットの最初の1バイトは47で始まる
								throw new IllegalStateException("MPEG2 TSでないファイル");
							}
							firstPacket = false;
							if (nowByte == 188 && buf[0] == 0x47) {
								// System.err.println("write");
								//mpeg2ts(buf);
								fileStream.write(buf, 0, nowByte);
							} else if (endByte) {
								// System.err.println("fileend MPEG2 TSパケットでない");
							} else {
								//fileStream.write(buf, 0, nowByte);
								System.err.println("MPEG2 TSパケットでない");
							}
						}
					}
					listener.progress(down.getTime(), time);
				}
			}

			status = Status.COMPLETE;
			listener.complete();
		} catch (Exception ex) {
			status = Status.ERROR;
			listener.exception(ex);
			listener.complete();
		}
	}

	private static class Property {

		private String type;
		private Map<String, String> properties;
		private String[] values;
		private String value;

		public String toString() {
			StringBuilder sb = new StringBuilder("Property type = ");
			sb.append(type);
			if (properties != null) {
				sb.append(", properties = { ");
				for (Map.Entry<String, String> i : properties.entrySet()) {
					sb.append(i.getKey());
					sb.append(" = ");
					sb.append(i.getValue());
					sb.append(", ");
				}
				sb.append("}");
			}
			if (values != null) {
				sb.append(", values = [ ");
				for (String i : values) {
					sb.append(i);
					sb.append(", ");
				}
				sb.append("]");
			}
			return sb.toString();
		}
	}

	private static final void mpeg2ts(byte[] b) {
		boolean tei = (0x80 & b[1]) > 0; // Transport Error Indicator  
		boolean pusi = (0x40 & b[1]) > 0; // Payload Unit Start Indicator
		boolean tp = (0x20 & b[1]) > 0; // Transport Priority
		int pid = ((0x1f & b[1]) * 256) + b[2]; // Adaptation field exist
		int sc = 0xc0 & b[3]; // Scrambling control
		boolean af = (0x20 & b[3]) > 0; // Adaptation field exist
		boolean cp = (0x10 & b[3]) > 0; // Contains payload
		int cc = (0x0f & b[3]); // Continuity counter

		System.err.print("PID:");
		System.err.print(pid);
		System.err.print(" CC:");
		System.err.print(cc);
		System.err.println();
	}

	private static final Property checkProperty(String line) {
		Property property = parseLine(line);
		if (property == null) return null;
		if (property.type.equals("FILE")) return property;
		if (property.type.equals("EXT-X-STREAM-INF") && property.properties != null) {
			// #EXT-X-STREAM-INF:BANDWIDTH=1280000,CODECS="...",AUDIO="aac"
			if (property.properties.get("BANDWIDTH") != null) {
				try {
					Long.parseLong(property.properties.get("BANDWIDTH"));
				} catch (NumberFormatException e) {
					System.err.println(property.toString());
					return null;
				}
			}
			return property;
		}
		if (property.type.equals("EXTINF") && property.values != null) {
			// #EXTINF:10.23,
			try {
				Double.parseDouble(property.values[0]);
			} catch (NumberFormatException e) {
				System.err.println(property.toString());
				return null;
			}
			return property;
		}
		if (property.type.equals("EXT-X-KEY") && property.properties != null) {
			// #EXT-X-KEY:METHOD=AES-128,URI=\"(.*)\",IV=0[xX](.{32})
			if (!"AES-128".equals(property.properties.get("METHOD"))) {
				System.err.println(property.toString());
				return null;
			}
			if (!property.properties.containsKey("URI")) {
				System.err.println(property.toString());
				return null;
			}
			if (property.properties.containsKey("IV") && !property.properties.get("IV").matches("0[xX](.{32})")) {
				System.err.println(property.toString());
				return null;
			}
			return property;
		}

		System.err.println(property.toString());
		return null;
	}

	private static final Property parseLine(String line) {
		Property ret = new Property();
		if (!line.startsWith("#")) {
			ret.type = "FILE";
			ret.value = line;
			return ret;
		}
		String[] types = line.split(":", 2);
		String type = types[0].substring(1);
		ret.type = type;
		if (types.length == 1) {
			return ret;
		}
		ret.value = types[1];
		ret.values = splitProperty(types[1]);
		ret.properties = new HashMap<>();
		for (String p : ret.values) {
			if (p.length() > 0) {
				String[] ps = p.split("=", 2);
				if (ps.length == 1) {
					ret.properties.put(p, null);
				} else {
					String k = ps[0];
					String v = ps[1];
					if (v.charAt(0) == '"' && v.charAt(v.length() - 1) == '"') {
						v = v.substring(1, v.length() - 1);
					}
					ret.properties.put(k, v);
				}
			}
		}
		return ret;
	}

	private static final String[] splitProperty(String line) {
		ArrayList<String> list = new ArrayList<>();
		boolean escape = false;
		boolean quote = false;
		StringBuilder sb = new StringBuilder();

		for (char c : line.toCharArray()) {
			if (escape) {
				escape = false;
			} else if (quote) {
				if (c == '"') quote = false;
			} else {
				if (c == ',') {
					list.add(sb.toString());
					sb.delete(0, sb.length());
					continue;
				}
				if (c == '\\') escape = true;
				if (c == '"') quote = true;
			}
			sb.append(c);
		}
		list.add(sb.toString());
		return list.toArray(new String[]{});
	}

}