/*******************************************************************************
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 ******************************************************************************/

package com.ericsson.oss.management.lcm.presentation.services.fileservice;

import java.nio.file.Path;
import java.util.Optional;

import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceHelmSourceUrl;
import org.springframework.web.multipart.MultipartFile;

/**
 * Working with files
 */
public interface FileService {

    /**
     * Create a directory
     *
     * @return the path to the directory
     */
    Path createDirectory();

    /**
     * Create a directory with the given name
     *
     * @param directoryName
     * @return path to the created directory
     */
    Path createDirectory(String directoryName);

    /**
     * Create directory with a predefined name in the predefined directory
     *
     * @param directory in which new directory will be created
     * @param name of the directory
     * @return created directory
     */
    Path createDirectory(Path directory, String name);

    /**
     * Get a certificates directory
     *
     * @return the path to the certificates directory
     */
    Optional<Path> getCertificatesDirectory();

    /**
     * Store the contents of a file
     *
     * @param directory to store the file in
     * @param file      which came in the request
     * @param filename  the name to give the file
     * @return the path to the file
     */
    Path storeFileIn(Path directory, MultipartFile file, String filename);

    /**
     * Store the contents of a file. This handles optional files in a request
     *
     * @param directory
     * @param file
     * @param filename
     * @return An Optional either containing the path or empty
     */
    Optional<Path> storeFileInIfPresent(Path directory, MultipartFile file, String filename);

    /**
     * Create the file in a specified folder and store the content
     * @param directory
     * @param content
     * @param filename
     * @return the path to the file
     */
    Path createFile(Path directory, byte[] content, final String filename);

    /**
     * Create the empty file in a specified folder
     * @param directory
     * @param filename
     * @return the path to the file
     */
    Path createEmptyFile(Path directory, String filename);

    /**
     * Store string content in already existing file
     *
     * @param path
     * @param content
     */
    void storeContentInFile(Path path, String content);


    /**
     * Delete a directory, and all it's contents
     *
     * @param directory to delete
     */
    void deleteDirectory(Path directory);

    /**
     * Delete a directory, and all it's contents if exists
     *
     * @param directory to delete
     */
    void deleteDirectoryIfExists(Path directory);

    /**
     * Extract the contents of an archive into the folder it is in
     *
     * @param archive the archive to extract
     * @param timeout
     */
    void extractArchive(Path archive, final int timeout);

    /**
     * Checks if archive contains the file with the specified name
     *
     * @param fileName
     * @param archive
     * @param timeout
     * @return result of the performed check
     */
    boolean checkFilePresenceInArchive(String fileName, Path archive, int timeout);

    /**
     * Copy the contents of one directory to another
     *
     * @param source directory of contents to copy
     * @param destination destination to copy contents to
     */
    void copyDirectoryContents(Path source, Path destination);

    /**
     * Read data from file
     *
     * @param file to read
     * @return string content of a file
     */
    String readDataFromFile(Path file);

    /**
     * Get file from path if it`s present
     * @param path
     * @return file
     */
    byte[] getFileContentIfPresent(Path path);

    /**
     * Get file from path
     * @param path
     * @return file
     */
    byte[] getFileContent(Path path);

    /**
     * Get file by name from directory
     *
     * @param directory to search in
     * @param filename of the file
     * @return path to the searched file
     */
    Path getFileFromDirectory(Path directory, String filename);

    /**
     * Delete file by path.
     *
     * @param file path to file which need to delete.
     */
    void deleteFile(Path file);

    /**
     * Get file by name and extension from directory
     *
     * @param directory to search in
     * @param filename of the file
     * @param fileExtension of the file
     * @return path to the searched file
     */
    String getDataFromDirectoryByNameAndExtension(Path directory, String filename, String fileExtension);

    /**
     * Get entity from directory
     *
     * @param directory to search in
     * @return entity from the searched file
     */
    WorkloadInstanceHelmSourceUrl getInstance(Path directory);

    /**
     * Copy a file or directory to target directory. Directories can be copied. However, files inside the directory are not copied,
     * so the new directory is empty even when the original directory contains files.
     * If the filename is null, it will be a copy of the file with the source filename.
     * @param sourceFile path to the file to be copied
     * @param targetDirectory path to target directory
     * @param fileName file copy name
     * @return path to copied file
     */
    Path copyFile(Path sourceFile, Path targetDirectory, String fileName);

    /**
     * Get any file from the directory by the specified extension. Note: the function does not search for files in internal directories.
     * @param directory path to target directory
     * @param fileExtension extension of file
     * @return file with provided extension or empty optional
     */
    Optional<Path> getFileFromDirectoryByExtensionIfPresent(Path directory, String fileExtension);

    /**
     * Check if directory exists
     *
     * @param directory path to checked directory
     * @return boolean result of checking
     */
    boolean fileExists(Path directory);

    /**
     * Read String from path
     *
     * @param path path to checked directory
     * @return string
     */
    String readStringFromPath(Path path);

    /**
     * Copy file from original to target path with replace existing if file with same name like target already exist
     *
     * @param pathToCopy path to original file to copy
     * @param targetPath path to target file to copy
     * @return Path to target file
     */
    Path copyFileWithReplaceExisting(Path pathToCopy, Path targetPath);
}
