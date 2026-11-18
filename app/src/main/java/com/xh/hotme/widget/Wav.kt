package com.macoli.wav_player

import java.io.DataInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
/**
 * WAV 文件
 * 只负责解析文件头，inputStream指针会自动跳过文件头
 * */
class Wav(private val inputStream : InputStream) {
    val wavHeader : WavHeader = WavHeader()
    init {
        parseHeader()
    }
    private fun parseHeader() {
        val dataInputStream : DataInputStream = DataInputStream(inputStream)

        val intValue = ByteArray(4)
        val shortValue = ByteArray(2)

        try {
            wavHeader.mChunkID = "" + Char(dataInputStream.readByte().toUShort()) + Char(
                dataInputStream.readByte().toUShort()
            ) + Char(dataInputStream.readByte().toUShort()) + Char(
                dataInputStream.readByte().toUShort()
            )
            dataInputStream.read(intValue)
            wavHeader.mChunkSize = byteArrayToInt(intValue)
            wavHeader.mFormat = "" + Char(dataInputStream.readByte().toUShort()) + Char(
                dataInputStream.readByte().toUShort()
            ) + Char(dataInputStream.readByte().toUShort()) + Char(
                dataInputStream.readByte().toUShort()
            )
            wavHeader.mSubChunk1ID = "" + Char(dataInputStream.readByte().toUShort()) + Char(
                dataInputStream.readByte().toUShort()
            ) + Char(dataInputStream.readByte().toUShort()) + Char(
                dataInputStream.readByte().toUShort()
            )
            dataInputStream.read(intValue)
            wavHeader.mSubChunk1Size = byteArrayToInt(intValue)
            dataInputStream.read(shortValue)
            wavHeader.mAudioFormat = byteArrayToShort(shortValue)
            dataInputStream.read(shortValue)
            wavHeader.mNumChannel = byteArrayToShort(shortValue)
            dataInputStream.read(intValue)
            wavHeader.mSampleRate = byteArrayToInt(intValue)
            dataInputStream.read(intValue)
            wavHeader.mByteRate = byteArrayToInt(intValue)
            dataInputStream.read(shortValue)
            wavHeader.mBlockAlign = byteArrayToShort(shortValue)
            dataInputStream.read(shortValue)
            wavHeader.mBitsPerSample = byteArrayToShort(shortValue)
            wavHeader.mSubChunk2ID = "" + Char(dataInputStream.readByte().toUShort()) + Char(
                dataInputStream.readByte().toUShort()
            ) + Char(dataInputStream.readByte().toUShort()) + Char(
                dataInputStream.readByte().toUShort()
            )
            dataInputStream.read(intValue)
            wavHeader.mSubChunk2Size = byteArrayToInt(intValue)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun byteArrayToShort(b: ByteArray): Short {
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).short
    }

    private fun byteArrayToInt(b: ByteArray): Int {
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int
    }

    /**
     * WAV文件头
     * */
    class WavHeader {
        var mChunkID = "RIFF"
        var mChunkSize = 0
        var mFormat = "WAVE"
        var mSubChunk1ID = "fmt "
        var mSubChunk1Size = 16
        var mAudioFormat: Short = 1
        var mNumChannel: Short = 1
        var mSampleRate = 8000
        var mByteRate = 0
        var mBlockAlign: Short = 0
        var mBitsPerSample: Short = 8
        var mSubChunk2ID = "data"
        var mSubChunk2Size = 0

        constructor()
        constructor(chunkSize: Int, sampleRateInHz: Int, channels: Int, bitsPerSample: Int) {
            mChunkSize = chunkSize
            mSampleRate = sampleRateInHz
            mBitsPerSample = bitsPerSample.toShort()
            mNumChannel = channels.toShort()
            mByteRate = mSampleRate * mNumChannel * mBitsPerSample / 8
            mBlockAlign = (mNumChannel * mBitsPerSample / 8).toShort()
        }

        override fun toString(): String {
            return "WavFileHeader{" +
                    "mChunkID='" + mChunkID + '\'' +
                    ", mChunkSize=" + mChunkSize +
                    ", mFormat='" + mFormat + '\'' +
                    ", mSubChunk1ID='" + mSubChunk1ID + '\'' +
                    ", mSubChunk1Size=" + mSubChunk1Size +
                    ", mAudioFormat=" + mAudioFormat +
                    ", mNumChannel=" + mNumChannel +
                    ", mSampleRate=" + mSampleRate +
                    ", mByteRate=" + mByteRate +
                    ", mBlockAlign=" + mBlockAlign +
                    ", mBitsPerSample=" + mBitsPerSample +
                    ", mSubChunk2ID='" + mSubChunk2ID + '\'' +
                    ", mSubChunk2Size=" + mSubChunk2Size +
                    '}'
        }
    }
}