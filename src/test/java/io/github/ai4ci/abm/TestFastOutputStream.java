package io.github.ai4ci.abm;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import io.github.ai4ci.util.FastWriteOnlyOutputStream;

class TestFastOutputStream {

	@Test
	void test() throws IOException {
		
		Path tmp = Paths.get("/tmp/test");
		FastWriteOnlyOutputStream out = new FastWriteOnlyOutputStream(tmp,1); 
		byte[] zeros = new byte[1024*1024*1024+123];
		System.out.println(System.currentTimeMillis());
		out.write(zeros);
		out.close();
		System.out.println(System.currentTimeMillis());
		// Files.delete(tmp);
	}

}
