package cn.wuyijun;

import java.lang.*;
import java.util.Enumeration;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.IntStream;

public class CustomStringTokenizer implements Enumeration<Object> {
    private int currentPosition;
    private int newPosition;
    private int maxPosition;
    private CustomString str;
    private String delimiters;
    private boolean retDelims;
    private boolean delimsChanged;

    private int maxDelimCodePoint;

    /**
     * Set maxDelimCodePoint to the highest char in the delimiter set.
     */
    private void setMaxDelimCodePoint() {
        int m = 0;
        for (int i = 0; i < delimiters.length(); i ++) {
            int c = delimiters.charAt(i);
            if (m < c)
                m = c;
        }
        maxDelimCodePoint = m;
    }

    public CustomStringTokenizer(CustomString str, String delim, boolean returnDelims) {
        currentPosition = 0;
        newPosition = -1;
        delimsChanged = false;
        this.str = str;
        maxPosition = str.length();
        delimiters = delim;
        retDelims = returnDelims;
        setMaxDelimCodePoint();
    }

    public CustomStringTokenizer(CustomString str, String delim) {
        this(str, delim, false);
    }

    private int skipDelimiters(int startPos) {
        int position = startPos;
        while (!retDelims && position < maxPosition) {
            char c = str.charAt(position);
            if ((c > maxDelimCodePoint) || (delimiters.indexOf(c) < 0))
                break;
            position++;
        }
        return position;
    }

    /**
     * Skips ahead from startPos and returns the index of the next delimiter
     * character encountered, or maxPosition if no such delimiter is found.
     */
    private int scanToken(int startPos) {
        int position = startPos;
        while (position < maxPosition) {
            char c = str.charAt(position);
            if ((c <= maxDelimCodePoint) && (delimiters.indexOf(c) >= 0))
                break;
            position++;
        }
        if (retDelims && (startPos == position)) {
            char c = str.charAt(position);
            if ((c <= maxDelimCodePoint) && (delimiters.indexOf(c) >= 0))
                position++;
        }
        return position;
    }

    public CustomString nextToken() {
        currentPosition = (newPosition >= 0 && !delimsChanged) ?
                newPosition : skipDelimiters(currentPosition);

        /* Reset these anyway */
        delimsChanged = false;
        newPosition = -1;

        if (currentPosition >= maxPosition)
            throw new NoSuchElementException();
        int start = currentPosition;
        currentPosition = scanToken(currentPosition);
        return str.substring(start, currentPosition);
    }

    public boolean hasMoreElements() {
        newPosition = skipDelimiters(currentPosition);
        return (newPosition < maxPosition);
    }

    public Object nextElement() {
        return nextToken();
    }

    public int countTokens() {
        int count = 0;
        int currpos = currentPosition;
        while (currpos < maxPosition) {
            currpos = skipDelimiters(currpos);
            if (currpos >= maxPosition)
                break;
            currpos = scanToken(currpos);
            count++;
        }
        return count;
    }
}

class CustomString implements CharSequence {
    private String str;

    public CustomString(String str) {
        this.str = str;
    }

    @Override
    public int length() {
        return str.length();
    }

    @Override
    public char charAt(int index) {
        return str.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return str.subSequence(start, end);
    }

    public CustomString substring(int start, int end) {
        return new CustomString(str.substring(start, end));
    }

    @Override
    public IntStream chars() {
        return str.chars();
    }

    @Override
    public IntStream codePoints() {
        return str.codePoints();
    }

    public CustomString trim() {
        return new CustomString(str.trim());
    }

    public CustomString toLowerCase() {
        return new CustomString(str.toLowerCase());
    }

}