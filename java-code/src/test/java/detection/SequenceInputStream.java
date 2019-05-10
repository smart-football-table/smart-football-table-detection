package detection;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

public class SequenceInputStream extends InputStream {

	private final Iterator<? extends InputStream> it;
	private InputStream in;

	public SequenceInputStream(Collection<? extends InputStream> streams) {
		this.it = streams.iterator();
		try {
			nextStream();
		} catch (IOException ex) {
			throw new Error("panic");
		}
	}

	protected void nextStream() throws IOException {
		if (in != null) {
			in.close();
		}
		if (it.hasNext()) {
			in = (InputStream) it.next();
			if (in == null)
				throw new NullPointerException();
		} else {
			in = null;
		}

	}

	public int available() throws IOException {
		return in == null ? 0 : in.available();
	}

	public int read() throws IOException {
		while (in != null) {
			int c = in.read();
			if (c != -1) {
				return c;
			}
			nextStream();
		}
		return -1;
	}

	public int read(byte b[], int off, int len) throws IOException {
		if (in == null) {
			return -1;
		} else if (b == null) {
			throw new NullPointerException();
		} else if (off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return 0;
		}
		do {
			int n = in.read(b, off, len);
			if (n > 0) {
				return n;
			}
			nextStream();
		} while (in != null);
		return -1;
	}

	public void close() throws IOException {
		do {
			nextStream();
		} while (in != null);
	}

}
