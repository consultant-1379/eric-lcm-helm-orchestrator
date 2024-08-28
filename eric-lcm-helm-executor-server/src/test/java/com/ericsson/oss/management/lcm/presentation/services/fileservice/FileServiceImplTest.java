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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;

import com.ericsson.oss.management.lcm.TestUtils;
import com.ericsson.oss.management.lcm.constants.HelmSourceConstants;
import com.ericsson.oss.management.lcm.presentation.exceptions.FileServiceException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandExecutorImpl;

@SpringBootTest(classes = {FileServiceImpl.class, CommandExecutorImpl.class})
class FileServiceImplTest {

    private static final String MOCK_MULTIPART_FILE_NAME = "TempFile";
    private static final String FOLDER_NAME = "special-name";
    private static final String TEMP_FILE_CONTENT = "Content";
    private static final String FILE_NAME = "MyTempFile";
    private static final String DIRECTORY = "helmfile-test";
    private static final String CRT_EXTENSION = ".crt";
    private static final Path CERTIFICATE_PATH = Paths.get("src/test/resources/test-charts/certificates");
    private static final String HOST_NAME = "ingress-demo";
    private static final String DATA_RESULT = "some certificate";
    private static final String TEST_YAML_FILE_NAME = "test.yaml";
    private static final String ERROR_MESSAGE = "You must specify a directory for the copy of the file. But %s is not a directory.";
    private static final String FILENAME_WITH_CONFIG_EXTENSION = "test.config";
    private static final String FILENAME_WITH_YAML_EXTENSION = "test.yaml";
    private static final String FILENAME_WITH_TGZ_EXTENSION = "test.tgz";
    private static final String CONFIG_EXTENSION = ".config";
    private static final String FILE_TO_COPY = "originalFileToCopy.txt";
    private static final String TARGET_FILE = "targetFileToMove.txt";

    @Autowired
    private FileServiceImpl fileService;

    @TempDir
    private Path folder;

    @BeforeEach
    void setUp() {
        setField(fileService, "rootDirectory", folder.toString());
    }

    @Test
    void successfullyCreateDirectory() {
        Path directory = fileService.createDirectory();
        assertThat(directory)
                .startsWith(Paths.get(System.getProperty("java.io.tmpdir")))
                .isEmptyDirectory();
    }

    @Test
    void failToCreateDirectory() throws IOException {
        deleteFolder();

        assertThatThrownBy(() -> fileService.createDirectory())
                .isInstanceOf(FileServiceException.class)
                .hasMessageContaining("Failed to create directory");
    }

    @Test
    void successfullyCreateDirectoryWithGivenNameAndDirectory() {
        Path directory = fileService.createDirectory();
        Path resultDirectory = fileService.createDirectory(directory, FOLDER_NAME);
        assertThat(resultDirectory).isEmptyDirectory();
        assertThat(resultDirectory.toString()).endsWith(FOLDER_NAME);
        fileService.deleteDirectory(directory);
    }

    @Test
    void failToCreateDirectoryWithGivenNameAndDirectory() throws IOException {
        deleteFolder();
        Path path = Path.of("some");
        assertThatThrownBy(() -> fileService.createDirectory(path, FOLDER_NAME))
                .isInstanceOf(FileServiceException.class)
                .hasMessageContaining("Failed to create directory");
    }

    @Test
    void successfullyStoreFile() {
        MockMultipartFile file = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, TEMP_FILE_CONTENT.getBytes());
        Path directory = fileService.createDirectory();
        Path storedFile = fileService.storeFileIn(directory, file, FILE_NAME);
        assertThat(storedFile)
                .exists()
                .hasContent(TEMP_FILE_CONTENT);
        assertThat(directory).isDirectoryContaining(path -> path
                .getFileName()
                .toString()
                .equalsIgnoreCase(FILE_NAME));
    }

    @Test
    void successfullyCreateDirectoryWithDirectoryName() {
        Path path = Paths.get(System.getProperty("java.io.tmpdir"));
        Path directory = fileService.createDirectory(DIRECTORY);

        assertThat(directory)
                .startsWith(path)
                .endsWith(Path.of(DIRECTORY))
                .isEmptyDirectory();
    }

    @Test
    void failToCreateDirectoryWithDirectoryName() throws IOException {
        deleteFolder();

        assertThatThrownBy(() -> fileService.createDirectory(DIRECTORY))
                .isInstanceOf(FileServiceException.class)
                .hasMessageContaining("Failed to create directory");
    }

    @Test
    void failToStoreFile() {
        MockMultipartFile file = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, TEMP_FILE_CONTENT.getBytes());
        Path path = Paths.get("non-Existent");
        assertThatThrownBy(() -> fileService.storeFileIn(path, file, "failed"))
                .isInstanceOf(FileServiceException.class)
                .hasMessageContaining("Failed to store file with a name failed");
    }

    @Test
    void successfullyDeleteDirectory() {
        Path directory = fileService.createDirectory();
        MockMultipartFile file = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, TEMP_FILE_CONTENT.getBytes());
        fileService.storeFileIn(directory, file, FILE_NAME);
        fileService.deleteDirectory(directory);
        assertThat(directory).doesNotExist();
    }

    @Test
    void failToDeleteDirectory() {
        Path directory = Paths.get("Non-existent");
        assertThatThrownBy(() -> fileService.deleteDirectory(directory))
                .isInstanceOf(FileServiceException.class)
                .hasMessageContaining("Failed to delete directory");
    }

    @Test
    void successfullyDeleteDirectoryIfExist() {
        Path directory = fileService.createDirectory();
        MockMultipartFile file = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, TEMP_FILE_CONTENT.getBytes());
        fileService.storeFileIn(directory, file, FILE_NAME);
        fileService.deleteDirectoryIfExists(directory);
        assertThat(directory).doesNotExist();
    }

    @Test
    void successfullyIgnoreDeleteDirectoryIfAbsent() {
        Path directory = Path.of("fake_path");
        fileService.deleteDirectoryIfExists(directory);
        assertThat(directory).doesNotExist();
    }

    @Test
    void successfullyExtractArchive() throws IOException {
        File archive = new ClassPathResource("helmsource-1.2.3-4.tgz").getFile();
        Path directory = fileService.createDirectory();
        MockMultipartFile file = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, new FileInputStream(archive));
        Path storedArchive = fileService.storeFileIn(directory, file, "helmsource-1.2.3-4.tgz");
        fileService.extractArchive(storedArchive, 5);
        assertThat(storedArchive.getParent().toFile().listFiles()).extracting(File::getName).contains("helmfile.yaml");
    }

    @Test
    void failToExtractArchive() throws IOException {
        File archive = new ClassPathResource("bad.tgz").getFile();
        Path directory = fileService.createDirectory();
        MockMultipartFile file = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, new FileInputStream(archive));
        Path storedArchive = fileService.storeFileIn(directory, file, "helmsource-1.2.3-4.tgz");
        assertThatThrownBy(() -> fileService.extractArchive(storedArchive, 5))
                .isInstanceOf(FileServiceException.class)
                .hasMessageContaining("Failed to extract archive");
    }

    @Test
    void successfullyGetFileFromDirectory() throws URISyntaxException {
        Path directory = TestUtils.getResource(DIRECTORY);

        Path result = fileService.getFileFromDirectory(directory, HelmSourceConstants.HELMFILE_YAML_FILENAME);
        assertThat(result.getFileName().toString()).isEqualTo(HelmSourceConstants.HELMFILE_YAML_FILENAME);
    }

    @Test
    void shouldFailWhenFileNotExists() throws URISyntaxException {
        Path directory = TestUtils.getResource(DIRECTORY);

        assertThatThrownBy(() -> fileService.getFileFromDirectory(directory, "not-existing.yaml"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void shouldFailWhenInvalidDirectory() {
        Path directory = Path.of("something");

        assertThatThrownBy(() -> fileService.getFileFromDirectory(directory, "some_name"))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldSuccessfulDeleteFileFromDirectory() throws IOException {
        Path testFile = folder.resolve("must_be_deleted.txt");
        Files.createFile(testFile);

        fileService.deleteFile(testFile);

        assertThat(testFile).doesNotExist();
    }

    @Test
    void shouldFailWhenTryToDeleteNonexistentFile() {
        Path nonexistentFile = Path.of("nonexistent");

        assertThatThrownBy(() -> fileService.deleteFile(nonexistentFile))
                .isInstanceOf(FileServiceException.class)
                .hasMessageContaining("Failed to delete file");
    }

    @Test
    void shouldCreateEmptyFile() {
        Path directory = fileService.createDirectory();
        Path emptyFile = fileService.createEmptyFile(directory, FILE_NAME);

        assertThat(emptyFile).exists();
        assertThat(directory).isDirectoryContaining(path -> path
                .getFileName()
                .toString()
                .equalsIgnoreCase(FILE_NAME));
    }

    @Test
    void shouldFailWhenFileNameIsEmpty() {
        Path directory = fileService.createDirectory();

        assertThatThrownBy(() -> fileService.createEmptyFile(directory, ""))
                .isInstanceOf(FileServiceException.class);
    }

    @Test
    void shouldCreateFileWithContent() {
        byte[] content = TEMP_FILE_CONTENT.getBytes();
        Path directory = fileService.createDirectory();

        Path file = fileService.createFile(directory, content, FILE_NAME);
        assertThat(file)
                .exists()
                .hasContent(TEMP_FILE_CONTENT);
    }

    @Test
    void shouldGetContentFromFile() throws IOException {
        Path directory = fileService.createDirectory();
        Path path = fileService.createEmptyFile(directory, MOCK_MULTIPART_FILE_NAME);
        Path file = Files.write(path, FILE_NAME.getBytes());
        byte[] expectedResult = FILE_NAME.getBytes();

        assertThat(fileService.getFileContent(file)).contains(expectedResult);
    }

    @Test
    void shouldFailGetContentFromFileWhenFileIsNotPresent() {
        Path file = Paths.get("");

        assertThatThrownBy(() -> fileService.getFileContent(file))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldGetContentFromFileIfPresent() throws IOException {
        Path directory = fileService.createDirectory();
        Path path = fileService.createEmptyFile(directory, MOCK_MULTIPART_FILE_NAME);
        Path file = Files.write(path, FILE_NAME.getBytes());
        byte[] expectedResult = FILE_NAME.getBytes();

        assertThat(fileService.getFileContentIfPresent(file)).contains(expectedResult);
    }

    @Test
    void shouldReadDataFromFile() throws IOException {
        Path directory = fileService.createDirectory();
        Path emptyFile = fileService.createEmptyFile(directory, MOCK_MULTIPART_FILE_NAME);
        Path file = Files.write(emptyFile, FILE_NAME.getBytes());

        assertThat(fileService.readDataFromFile(file)).contains(FILE_NAME);
    }

    @Test
    void shouldFailReadDataFromFileWhenFileNotExist() throws IOException {
        Path directory = fileService.createDirectory();
        deleteFolder();

        assertThatThrownBy(() -> fileService.readDataFromFile(directory))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void shouldCopyContentFromOneDirectoryToAnotherDirectory() throws IOException {
        Path firstDirectory = fileService.createDirectory();
        Path emptyFile = fileService.createEmptyFile(firstDirectory, MOCK_MULTIPART_FILE_NAME);
        Files.write(emptyFile, FILE_NAME.getBytes());
        Path secondDirectory = fileService.createDirectory();

        fileService.copyDirectoryContents(firstDirectory, secondDirectory);

        assertThat(secondDirectory)
                .isNotEmptyDirectory()
                .isDirectoryContaining(path -> path
                .getFileName()
                .toString()
                .equalsIgnoreCase(MOCK_MULTIPART_FILE_NAME));
    }

    @Test
    void shouldFailContentFromDirectoriesWhenFolderIsEmpty() throws IOException {
        Path firstDirectory = fileService.createDirectory();
        Path secondDirectory = fileService.createDirectory();
        deleteFolder();

        assertThatThrownBy(() -> fileService.copyDirectoryContents(firstDirectory, secondDirectory))
                .isInstanceOf(InternalRuntimeException.class);
    }

    @Test
    void shouldStoreContentInFile() {
        Path firstDirectory = fileService.createDirectory();
        Path file = fileService.createEmptyFile(firstDirectory, MOCK_MULTIPART_FILE_NAME);
        fileService.storeContentInFile(file, TEMP_FILE_CONTENT);

        assertThat(file).hasContent(TEMP_FILE_CONTENT);
    }

    @Test
    void shouldFailStoreContentInFile() {
        Path file = Paths.get("");

        assertThatThrownBy(() -> fileService.storeContentInFile(file, TEMP_FILE_CONTENT))
                .isInstanceOf(FileServiceException.class)
                .hasMessageContaining("Failed to save content in a file");
    }

    @Test
    void shouldStoreContentIfFileIsPresent() {
        MockMultipartFile file = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, TEMP_FILE_CONTENT.getBytes());
        Path directory = fileService.createDirectory();
        Optional<Path> storedFile = fileService.storeFileInIfPresent(directory, file, FILE_NAME);
        assertThat(storedFile.get())
                .exists()
                .hasContent(TEMP_FILE_CONTENT);
    }

    @Test
    void shouldCheckFileInArchive() throws IOException {
        File archive = new ClassPathResource("helmsource-1.2.3-4.tgz").getFile();
        Path directory = fileService.createDirectory();
        MockMultipartFile file = new MockMultipartFile(MOCK_MULTIPART_FILE_NAME, new FileInputStream(archive));
        Path storedArchive = fileService.storeFileIn(directory, file, "helmsource-1.2.3-4.tgz");

        assertThat(fileService.checkFilePresenceInArchive("helmfile.yaml", storedArchive, 5)).isTrue();
    }

    @Test
    void shouldGetDataFromDirectoryByNameAndExtension() {
        String result = fileService.getDataFromDirectoryByNameAndExtension(CERTIFICATE_PATH, HOST_NAME, CRT_EXTENSION);

        assertThat(result).isEqualTo(DATA_RESULT);
    }

    @Test
    void shouldGetDataFromDirectoryByNameWithoutExtension() {
        assertThatThrownBy(() -> fileService.getDataFromDirectoryByNameAndExtension(CERTIFICATE_PATH, HOST_NAME, FILE_NAME))
                .isInstanceOf(FileServiceException.class);
    }

    @Test
    void shouldSuccessfulCopyFileNameAndFileContentToTargetDirectoryIfFileNameNotDefined() throws IOException {
        Path testTempFile = folder.resolve(TEST_YAML_FILE_NAME);
        Files.createFile(testTempFile);
        Files.write(testTempFile, TEMP_FILE_CONTENT.getBytes());
        Path tempDirectory = folder.resolve("tempDirectory");
        Files.createDirectory(tempDirectory);

        Path result = fileService.copyFile(testTempFile, tempDirectory, null);

        assertThat(result)
                .exists()
                .hasParent(tempDirectory)
                .hasSameTextualContentAs(testTempFile)
                .hasFileName(TEST_YAML_FILE_NAME);
    }

    @Test
    void shouldCreateEmptyDirectoryWithTheSameNameIfFileToCopyIsDirectory() throws IOException {
        String nameOfCopiedDirectory = "copiedDirectory";
        Path folderToCopy = folder.resolve(nameOfCopiedDirectory);
        Files.createDirectory(folderToCopy);
        Files.createFile(folderToCopy.resolve(FILE_NAME));
        Path destinationFolder = folder.resolve("destinationDirectory");
        Files.createDirectory(destinationFolder);

        Path result = fileService.copyFile(folderToCopy, destinationFolder, null);

        assertThat(result)
                .exists()
                .hasFileName(nameOfCopiedDirectory)
                .hasParent(destinationFolder)
                .isEmptyDirectory();
    }

    @Test
    void shouldCreateCopyWithDefinedName() throws IOException {
        String definedName = "another-name.yaml";
        Path testTempFile = folder.resolve(TEST_YAML_FILE_NAME);
        Files.createFile(testTempFile);
        Files.write(testTempFile, TEMP_FILE_CONTENT.getBytes());
        Path tempDirectory = folder.resolve("tempDirectory");
        Files.createDirectory(tempDirectory);

        Path result = fileService.copyFile(testTempFile, tempDirectory, definedName);

        assertThat(result)
                .exists()
                .hasParent(tempDirectory)
                .hasFileName(definedName)
                .hasSameTextualContentAs(testTempFile);
    }

    @Test
    void shouldThrowExceptionIfFileWithTheSameNameAlreadyExistsInTargetDirectory() throws IOException {
        Path fileToCopy = folder.resolve(TEST_YAML_FILE_NAME);
        Files.createFile(fileToCopy);
        Path destinationFolder = folder.resolve("destinationFolder");
        Files.createDirectory(destinationFolder);

        Files.createFile(destinationFolder.resolve(TEST_YAML_FILE_NAME));

        assertThatThrownBy(() -> fileService.copyFile(fileToCopy, destinationFolder, TEST_YAML_FILE_NAME))
                .isInstanceOf(FileServiceException.class)
                .hasMessageContaining(String.format("Something went wrong during copying file %s", fileToCopy.getFileName()));
    }

    @Test
    void shouldThrowExceptionIfPathToPlaceForCopyFileIsNotDirectory() throws IOException {
        Path testFile = folder.resolve("test1.txt");
        Files.createFile(testFile);
        Path wrongPathDirectory = folder.resolve("test2.txt");
        Files.createFile(wrongPathDirectory);

        assertThatThrownBy(() -> fileService.copyFile(testFile, wrongPathDirectory, null))
                .isInstanceOf(FileServiceException.class)
                .hasMessageContaining(String.format(ERROR_MESSAGE, wrongPathDirectory.getFileName()));
    }

    @Test
    void shouldReturnOptionalWithPathToFileWitConfigExtension() throws IOException {
        Files.createFile(folder.resolve(FILENAME_WITH_CONFIG_EXTENSION));
        Files.createFile(folder.resolve(FILENAME_WITH_YAML_EXTENSION));
        Files.createFile(folder.resolve(FILENAME_WITH_TGZ_EXTENSION));

        Optional<Path> result = fileService.getFileFromDirectoryByExtensionIfPresent(folder, CONFIG_EXTENSION);

        assertThat(result).isNotEmpty();
        assertThat(result.get()).hasFileName(FILENAME_WITH_CONFIG_EXTENSION);
    }

    @Test
    void shouldReturnEmptyOptionalIfFileWithExtensionNotFound() throws IOException {
        Files.createFile(folder.resolve(FILENAME_WITH_YAML_EXTENSION));
        Files.createFile(folder.resolve(FILENAME_WITH_TGZ_EXTENSION));

        Optional<Path> result = fileService.getFileFromDirectoryByExtensionIfPresent(folder, CONFIG_EXTENSION);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotSearchFilesWithExtensionInTheInnerDirectories() throws IOException {
        Path innerDirectory = Files.createDirectory(folder.resolve(FOLDER_NAME));
        Files.createFile(innerDirectory.resolve(FILENAME_WITH_CONFIG_EXTENSION));
        Files.createFile(folder.resolve(FILENAME_WITH_YAML_EXTENSION));

        Optional<Path> result = fileService.getFileFromDirectoryByExtensionIfPresent(folder, CONFIG_EXTENSION);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldThrowExceptionIfFileExtensionIsInvalid() {
        assertThatThrownBy(() -> fileService.getFileFromDirectoryByExtensionIfPresent(folder, "invalid"))
                .isInstanceOf(FileServiceException.class)
                .hasMessageContaining("This is not file extension, please check");
    }

    private void deleteFolder() throws IOException {
        Files.walk(folder)
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    void shouldCopyFileWithReplaceExisting() {
        Path directory = fileService.createDirectory("files-to-copy");
        Path originFile = fileService.createEmptyFile(directory, FILE_TO_COPY);
        Path targetFile = fileService.createEmptyFile(directory, TARGET_FILE);

        Path result = fileService.copyFileWithReplaceExisting(originFile, targetFile);

        assertThat(result.toString()).isEqualTo(targetFile.toString());
    }
}
