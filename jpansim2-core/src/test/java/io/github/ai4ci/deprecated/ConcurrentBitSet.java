package io.github.ai4ci.deprecated;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.function.Function;

@Deprecated
public class ConcurrentBitSet {

	    private static final int WORD_SIZE = Byte.SIZE;
	    
	    private final AtomicWordArray data;
	    
	    public ConcurrentBitSet(int size) {
	        this.data = new AtomicWordArray((size + WORD_SIZE - 1) / WORD_SIZE);
	    }

	    public boolean get(int bitIndex) {
	        int wordIndex = bitIndex / WORD_SIZE;
	        int bitOffset = bitIndex % WORD_SIZE;
	        int bitMask = 1 << bitOffset;
	        return (this.data.getWordVolatile(wordIndex) & bitMask) >> bitOffset != 0;
	    }

	    public void set(int bitIndex) {
	        int wordIndex = bitIndex / WORD_SIZE;
	        int bitOffset = bitIndex % WORD_SIZE;
	        int bitMask = 1 << bitOffset;
	        this.data.setWordVolatile(wordIndex, (word) -> (byte) (word | bitMask));
	    }

	    public void clear(int bitIndex) {
	        int wordIndex = bitIndex / WORD_SIZE;
	        int bitOffset = bitIndex % WORD_SIZE;
	        int bitMask = 1 << bitOffset;
	        this.data.setWordVolatile(wordIndex, (word) -> (byte) (word & ~bitMask));
	    }

	    public void flip(int bitIndex) {
	        int wordIndex = bitIndex / WORD_SIZE;
	        int bitOffset = bitIndex % WORD_SIZE;
	        int bitMask = 1 << bitOffset;
	        this.data.setWordVolatile(wordIndex, (word) -> (byte) (word ^ bitMask));
	    }

	    private static class AtomicWordArray {
	        private static final VarHandle WORDS_VAR_HANDLER = MethodHandles.arrayElementVarHandle(byte[].class);
	        
	        private final byte[] words;
	        
	        private AtomicWordArray(int size) {
	            this.words = new byte[size];
	        }

	        private void setWordVolatile(int wordIndex, Function<Byte, Byte> binaryOperation) {
	            byte currentWordValue;
	            byte newWordValue;
	            
	            do {
	                currentWordValue = this.getWordVolatile(wordIndex);
	                newWordValue = binaryOperation.apply(currentWordValue);
	            } while (!WORDS_VAR_HANDLER.compareAndSet(this.words, wordIndex, currentWordValue, newWordValue));
	        }
	  
	        private byte getWordVolatile(int wordIndex) {
	            return (byte) WORDS_VAR_HANDLER.getVolatile(this.words, wordIndex);
	        }
	    }
	}

