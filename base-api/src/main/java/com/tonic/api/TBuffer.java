package com.tonic.api;

public interface TBuffer
{
    /**
     * Gets the underlying byte array.
     *
     * @return the byte array
     */
    byte[] getArray();
    
    /**
     * Gets the current offset in the buffer.
     *
     * @return the offset
     */
    int getOffset();
    
    /**
     * Writes a byte value to the buffer.
     *
     * @param var the byte value to write
     */
    void writeByte(int var);
    
    /**
     * Writes a byte value with addition transformation.
     *
     * @param var the byte value to write
     */
    void writeByteAdd(int var);
    
    /**
     * Writes a byte value with negation transformation.
     *
     * @param var the byte value to write
     */
    void writeByteNeg(int var);
    
    /**
     * Writes a byte value with subtraction transformation.
     *
     * @param var the byte value to write
     */
    void writeByteSub(int var);

    /**
     * Writes a short value to the buffer.
     *
     * @param var the short value to write
     */
    void writeShort(int var);
    
    /**
     * Writes a short value in little-endian format.
     *
     * @param var the short value to write
     */
    void writeShortLE(int var);
    
    /**
     * Writes a short value with addition transformation.
     *
     * @param var the short value to write
     */
    void writeShortAdd(int var);
    
    /**
     * Writes a short value with addition transformation in little-endian format.
     *
     * @param var the short value to write
     */
    void writeShortAddLE(int var);

    /**
     * Writes an integer in mixed-endian format.
     *
     * @param var the integer value to write
     */
    void writeIntME(int var);
    
    /**
     * Writes an integer in little-endian format.
     *
     * @param var the integer value to write
     */
    void writeIntLE(int var);
    
    /**
     * Writes an integer to the buffer.
     *
     * @param var the integer value to write
     */
    void writeInt(int var);
    
    /**
     * Writes an integer in inverted mixed-endian format.
     *
     * @param var the integer value to write
     */
    void writeIntIME(int var);

    /**
     * Writes a length byte to the buffer.
     *
     * @param var the length value to write
     */
    void writeLengthByte(int var);
    
    /**
     * Writes a null-terminated string in CP1252 encoding.
     *
     * @param var the string to write
     */
    void writeStringCp1252NullTerminated(String var);
    
    /**
     * Writes a null-circumfixed string in CP1252 encoding.
     *
     * @param var the string to write
     */
    void writeStringCp1252NullCircumfixed(String var);
}
