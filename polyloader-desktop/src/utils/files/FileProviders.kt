package org.im.polyloader.utils.files

import java.io.File
import java.io.InputStream
import java.util.zip.ZipFile

interface IFileProvider {
    fun exists(path: String): Boolean
    fun getFileStream(path: String) : InputStream
}

class ArchiveFileProvider(archiveFile: File) : IFileProvider {
    private val zipFile: ZipFile = ZipFile(archiveFile)
    override fun exists(path: String): Boolean =
        zipFile.getEntry(path) != null
    override fun getFileStream(path: String): InputStream =
        zipFile.getInputStream(zipFile.getEntry(path))
}

class DirectoryFileProvider(private val directory: File) : IFileProvider {
    override fun exists(path: String): Boolean =
        File(directory, path).exists()
    override fun getFileStream(path: String): InputStream =
        File(directory, path).inputStream()
}