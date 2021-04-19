package com.microsoft.office.zip

import com.microsoft.office.zip.internal.ZipReaderImpl
import com.microsoft.office.zip.internal.cache.MemoryCache
import com.microsoft.office.zip.internal.input.FileRandomAccessInput
import com.microsoft.office.zip.internal.input.HttpRandomAccessInput
import com.microsoft.office.zip.internal.input.RandomAccessInput
import java.io.InputStream
import java.util.zip.CRC32
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockWebServer
import org.hamcrest.Matcher
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.AfterClass
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
internal class ZipReaderTest(private val parameter: Parameter) {

    internal interface Parameter {
        val input: RandomAccessInput
        val files: List<String>
        val fileMatcher: Matcher<Iterable<String>>
    }

    companion object {
        @JvmStatic
        private val mockWebServer: MockWebServer = MockWebServer()

        @JvmStatic
        @BeforeClass
        fun onSetup() {
            mockWebServer.dispatcher = FileDispatcher()
            mockWebServer.start()
        }

        @JvmStatic
        @AfterClass
        fun onTearDown() {
            mockWebServer.shutdown()
        }

        private fun compareFiles(fileName: String, inputStream: InputStream): Boolean {
            inputStream.use { stream ->
                val buf1 = Resources.file(fileName).readBytes()
                val buf2 = stream.readBytes()

                return buf1.contentEquals(buf2)
            }
        }

        private fun validateChecksum(inputStream: InputStream, crc32: Long): Boolean {
            inputStream.use { stream ->
                val checksum = CRC32()
                checksum.update(stream.readBytes())
                return checksum.value == crc32
            }
        }

        @JvmStatic
        @Parameters
        fun data(): Collection<Parameter> {
            // Parameters for a plain old ZIP file.
            val zipFileParams = object : Parameter {
                override val input: RandomAccessInput
                    get() = FileRandomAccessInput(Resources.file("test.zip"))

                override val files: List<String> = listOf(
                    "folder/lipsum.txt",
                    "folder/lorem.txt",
                    "lipsum.txt",
                    "lorem.txt"
                )

                override val fileMatcher: Matcher<Iterable<String>> = containsInAnyOrder(
                    "folder/",
                    "folder/lipsum.txt",
                    "folder/lorem.txt",
                    "lipsum.txt",
                    "lorem.txt"
                )
            }

            val appxFilesInPackage = listOf(
                "AppxBlockMap.xml",
                "[Content_Types].xml",
                "folder/lipsum.txt",
                "folder/lorem.txt",
                "lipsum.txt",
                "lorem.txt",
                "test.png"
            )

            val appxFileMatcher = containsInAnyOrder(
                "AppxManifest.xml",
                "AppxBlockMap.xml",
                "[Content_Types].xml",
                "folder/lipsum.txt",
                "folder/lorem.txt",
                "lipsum.txt",
                "lorem.txt",
                "test.png"
            )

            // Parameters for AppX file on disk.
            val appxFileParams = object : Parameter {
                override val input: RandomAccessInput
                    get() = FileRandomAccessInput(Resources.file("test.appx"))
                override val files: List<String> = appxFilesInPackage
                override val fileMatcher: Matcher<Iterable<String>> = appxFileMatcher
            }

            // Parameters for AppX file from a remote server.
            val urlAppxFileParams = object : Parameter {
                override val input: RandomAccessInput
                    get() {
                        val url = mockWebServer.url("/file/test.appx")
                        return HttpRandomAccessInput(
                            url.toUrl(),
                            OkHttpClient.Builder().build()
                        )
                    }

                override val files: List<String> = appxFilesInPackage
                override val fileMatcher: Matcher<Iterable<String>> = appxFileMatcher
            }

            return listOf(zipFileParams, appxFileParams, urlAppxFileParams)
        }
    }

    @Test
    fun zip_containsExpectedFiles() {
        ZipReaderImpl(parameter.input, MemoryCache()).use { reader ->
            assertThat(
                "ZIP does not contain expected files",
                reader.files,
                parameter.fileMatcher
            )
        }
    }

    @Test
    fun zip_file_contentsMatch() {
        ZipReaderImpl(parameter.input, MemoryCache()).use { reader ->
            val files = reader.fileStreams(parameter.files)
            files.forEach { (fileName, inputStream) ->
                assertTrue(
                    "Contents of $fileName do not match",
                    compareFiles(fileName, inputStream)
                )
            }
        }
    }

    @Test
    fun zip_file_checksumsMatch() {
        ZipReaderImpl(parameter.input, MemoryCache()).use { reader ->
            val files = reader.fileStreams(parameter.files)
            files.forEach { (fileName, inputStream) ->
                val metadata = reader.fileMetadata(fileName)
                assertTrue(
                    "Checksum of $fileName does not match",
                    validateChecksum(inputStream, metadata.crc32)
                )
            }
        }
    }
}
