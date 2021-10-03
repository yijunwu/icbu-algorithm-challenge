package cn.wuyijun;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The byte tokenizer class allows an application to break a byte array into
 * tokens.
 *
 * @author <a href = "mailto:doquocduy@gmail.com">Duy Do</a>
 * @version Sep 23, 2008 12:49:06 PM
 */
public class ByteTwoLevelsTokenizer implements Enumeration<Object> {

    private int currentPosition;
    private final int maxPosition;

    private final byte[] bytes;
    private final byte delimiterL1;
    private final byte delimiterL2;

    /**
     * Constructs a bytes array tokenizer for the specified bytes. The byte
     * <code>delimiter</code> argument is the delimiter for separating tokens.
     * Delimiter byte itself will not be treated as tokens.
     * <p>
     * Note that if <tt>delimiter</tt> is <tt>null</tt>, this constructor does
     * not throw an exception. However, trying to invoke other methods on the
     * resulting <tt>ByteTokenizer</tt> may result in a
     * <tt>NullPointerException</tt>.
     *
     * @param bytes a bytes array to be parsed
     * @param delimiterL2 a byte delimiter
     * @exception NullPointerException if bytes is <CODE>null</CODE>
     */
    public ByteTwoLevelsTokenizer(byte[] bytes, byte delimiterL1, byte delimiterL2) {
        this.bytes = bytes;
        this.delimiterL1 = delimiterL1;
        this.delimiterL2 = delimiterL2;
        currentPosition = 0;
        maxPosition = bytes.length;
    }

    /**
     * Tests if there are more tokens available from this tokenizer's bytes
     * array. If this method returns <tt>true</tt>, then a subsequent call to
     * <tt>nextToken</tt> with no argument will successfully return a token.
     *
     * @return <code>true</code> if there are more tokens; <code>false</code>
     *         otherwise.
     */
    public boolean hasMoreTokens() {
        int newPosition = skipDelimiters(currentPosition);
        return (newPosition < maxPosition);
    }

    /**
     * Returns the same value as the <code>hasMoreTokens</code> method. It
     * exists so that this class can implement the <code>Enumeration</code>
     * interface.
     *
     * @return <code>true</code> if there are more tokens; <code>false</code>
     *         otherwise.
     * @see Enumeration
     * @see ByteTwoLevelsTokenizer#hasMoreTokens()
     */
    public boolean hasMoreElements() {
        return hasMoreTokens();
    }

    /**
     * Returns the next token from this bytes array tokenizer.
     *
     * @return the next token from this bytes array tokenizer.
     * @exception NoSuchElementException if there are no more tokens in this
     *                tokenizer's bytes array.
     */
    public List<ByteBuffer> nextToken() {
        List<ByteBuffer> result = new ArrayList<>();
        while (hasMoreTokens()) {
            ByteBuffer byteBuffer = this.nextL2Token();
            if (byteBuffer == null) {
                return result;
            } else {
                result.add(byteBuffer);
            }
        }
        return result;
    }

    private ByteBuffer nextL1OrL2Token() {
        currentPosition = skipDelimiters(currentPosition);

        if (currentPosition >= maxPosition) {
            throw new NoSuchElementException(
                    "current token position is out of bound.");
        }
        final int startPosition = currentPosition;
        currentPosition = scanToken(currentPosition);
        final int length = currentPosition - startPosition;
        return ByteBuffer.wrap(bytes, startPosition, length);
    }

    private ByteBuffer nextL2Token() {
        currentPosition = skipL2Delimiters(currentPosition);

        if (currentPosition >= maxPosition) {
            throw new NoSuchElementException(
                    "current token position is out of bound.");
        }

        if (bytes[currentPosition] == delimiterL1) { currentPosition ++; return null; }

        final int startPosition = currentPosition;
        currentPosition = scanToken(currentPosition);
        final int length = currentPosition - startPosition;
        return ByteBuffer.wrap(bytes, startPosition, length);
    }

    /**
     * Returns the same value as the <code>nextToken</code> method, except that
     * its declared return value is <code>Object</code> rather than
     * <code>String</code>. It exists so that this class can implement the
     * <code>Enumeration</code> interface.
     *
     * @return the next token in the bytes array.
     * @exception NoSuchElementException if there are no more tokens in this
     *                tokenizer's bytes array.
     * @see Enumeration
     * @see ByteTwoLevelsTokenizer#nextToken()
     */
    public Object nextElement() {
        return nextToken();
    }

    /**
     * Calculates the number of times that this tokenizer's
     * <code>nextToken</code> method can be called before it generates an
     * exception. The current position is not advanced.
     *
     * @return the number of tokens remaining in the bytes array using the
     *         current delimiter set.
     * @see ByteTwoLevelsTokenizer#nextToken()
     */
    public int countTokens() {
        int tokenNums = 0;
        int position = currentPosition;
        while (position < maxPosition) {
            position = skipDelimiters(position);
            if (position >= maxPosition) {
                break;
            }
            position = scanToken(position);
            tokenNums++;
        }
        return tokenNums;
    }

    /**
     * Skips delimiters starting from the specified position. Returns the index
     * of the first non-delimiter byte at or after startPosition.
     */
    private int skipDelimiters(final int startPosition) {
        int position = startPosition;
        while (position < maxPosition) {
            if (isDelimiter(position)) {
                position += 1;
            } else {
                break;
            }
        }
        return position;
    }

    private int skipL2Delimiters(final int startPosition) {
        int position = startPosition;
        while (position < maxPosition) {
            if (isL2Delimiter(position)) {
                position += 1;
            } else {
                break;
            }
        }
        return position;
    }

    /**
     * Skips ahead from startPosition and returns the index of the next
     * delimiter byte encountered, or maxPosition if no such delimiter is found.
     */
    private int scanToken(final int startPosition) {
        int position = startPosition;
        while (position < maxPosition) {
            if (isDelimiter(position)) {
                break;
            }
            position++;
        }
        return position;
    }

    private boolean isDelimiter(final int startPosition) {
        return bytes[startPosition] == delimiterL1 || bytes[startPosition] == delimiterL2;
    }
    private boolean isL2Delimiter(final int startPosition) {
        return bytes[startPosition] == delimiterL2;
    }

}