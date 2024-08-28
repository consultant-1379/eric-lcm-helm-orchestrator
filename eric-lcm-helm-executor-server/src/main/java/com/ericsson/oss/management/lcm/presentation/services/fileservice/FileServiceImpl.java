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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Comparator.reverseOrder;

import static com.ericsson.oss.management.lcm.constants.CommandConstants.EXTRACT_ARCHIVE_COMMAND;
import static com.ericsson.oss.management.lcm.constants.CommandConstants.FETCH_ARCHIVE_CONTENT;
import static com.ericsson.oss.management.lcm.constants.FileDetails.DOT_DELIMITER;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.ericsson.oss.management.lcm.model.entity.WorkloadInstanceHelmSourceUrl;
import com.ericsson.oss.management.lcm.presentation.exceptions.FileServiceException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InternalRuntimeException;
import com.ericsson.oss.management.lcm.presentation.exceptions.InvalidFileException;
import com.ericsson.oss.management.lcm.presentation.exceptions.ResourceNotFoundException;
import com.ericsson.oss.management.lcm.presentation.exceptions.SecurityOperationException;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandExecutor;
import com.ericsson.oss.management.lcm.utils.command.executor.CommandResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    @Value("${directory.root}")
    private String rootDirectory;
    @Value("${directory.certificates}")
    private String certificatesDirectory;

    private final CommandExecutor commandExecutor;

    @Override
    public Path createDirectory() {
        return createDirectory(UUID.randomUUID().toString());
    }

    @Override
    public Path createDirectory(String directoryName) {
        log.info(String.format("Creating directory with the given name %s", directoryName));
        try {
            var directory = Paths.get(rootDirectory).resolve(directoryName);
            return Files.createDirectory(directory);
        } catch (IOException e) {
            throw new FileServiceException(String.format("Failed to create directory with name %s.",
                                                         directoryName));
        }
    }

    @Override
    public Optional<Path> getCertificatesDirectory() {
        log.info("Get a certificates directory");
        try {
            return Optional.of(Paths.get(certificatesDirectory));
        } catch (RuntimeException e) {
            throw new FileServiceException("Failed to find certificates directory");
        }
    }

    @Override
    public Path createDirectory(Path directory, String name) {
        log.info("Creating directory in the given directory with a given name");
        try {
            var newDirectory = Paths.get(directory.toString()).resolve(name);
            return Files.createDirectory(newDirectory);
        } catch (IOException e) {
            throw new FileServiceException(String.format("Failed to create directory with a name %s", name));
        }
    }

    @Override
    public Path storeFileIn(final Path directory, final MultipartFile file, final String filename) {
        log.info("Storing {} in {}", filename, directory);
        var destination = directory.resolve(filename);
        try {
            file.transferTo(destination);
        } catch (IOException e) {
            throw new FileServiceException(String.format("Failed to store file with a name %s", filename));
        }
        return destination;
    }

    @Override
    public Optional<Path> storeFileInIfPresent(final Path directory, final MultipartFile file, final String filename) {
        return Optional
                .ofNullable(file)
                .map(existingFile -> storeFileIn(directory, existingFile, filename));
    }

    @Override
    public void storeContentInFile(final Path path, final String content) {
        try {
            FileUtils.writeStringToFile(path.toFile(), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileServiceException(String.format("Failed to save content in a file %s", path.getFileName()));
        }
    }

    @Override
    public Path createFile(Path directory, byte[] content, final String filename) {
        log.info("Creating {} in {}", filename, directory);
        Path file = createEmptyFile(directory, filename);
        try {
            Files.write(file, content);
        } catch (IOException e) {
            throw new FileServiceException(String.format("Failed to create file with a name %s", filename));
        }
        return file;
    }

    @Override
    public Path createEmptyFile(Path directory, String filename) {
        log.info("Creating empty {} in {}", filename, directory);
        var destination = directory.resolve(filename);

        try {
            Files.createFile(destination);
        } catch (IOException e) {
            throw new FileServiceException(String.format("Failed to create empty file with a name %s", filename));
        }
        return destination;
    }

    @Override
    public void deleteDirectory(final Path directory) {
        log.info("Will delete {}", directory);

        deleteFiles(directory);
    }

    @Override
    public void deleteDirectoryIfExists(Path directory) {
        log.info("Will delete {}", directory);
        deleteFilesIfExists(directory);
    }

    @Override
    public void extractArchive(final Path archive, final int timeout) {
        var command = String.format(EXTRACT_ARCHIVE_COMMAND, archive, archive.getParent());
        CommandResponse response = commandExecutor.execute(command, timeout);
        if (response.getExitCode() != 0) {
            throw new FileServiceException(String.format("Failed to extract archive due to %s", response.getOutput()));
        }
    }

    @Override
    public boolean checkFilePresenceInArchive(String fileName, Path archive, int timeout) {
        var command = String.format(FETCH_ARCHIVE_CONTENT, archive);
        CommandResponse response = commandExecutor.execute(command, timeout);
        if (response.getExitCode() != 0) {
            throw new FileServiceException(String.format("Failed to check file presence in archive due to %s", response.getOutput()));
        }
        return response.getOutput().contains(fileName);
    }

    @Override
    public void copyDirectoryContents(final Path sourceDirectoryLocation, final Path destinationDirectoryLocation) {
        log.info("Will copy contents of {} to {}", sourceDirectoryLocation, destinationDirectoryLocation);
        try {
            FileUtils.copyDirectory(sourceDirectoryLocation.toFile(), destinationDirectoryLocation.toFile());
        } catch (IOException e) {
            throw new InternalRuntimeException("Could not copy contents of directory");
        }
    }

    @Override
    public String readDataFromFile(final Path file) {
        try (Stream<String> lines = Files.lines(file)) {
            return lines.collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new InvalidFileException(String.format(
                    "Can't read file %s.", file.getFileName().toString()));
        }
    }

    @Override
    public byte[] getFileContentIfPresent(Path path) {
        return Optional.ofNullable(path)
                .map(this::getFileContent)
                .orElse(null);
    }

    @Override
    public byte[] getFileContent(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new InvalidFileException(String.format("Something went wrong during reading file %s", path.getFileName()));
        }
    }

    @Override
    public Path getFileFromDirectory(Path directory, String filename) {
        try (Stream<Path> walk = Files.walk(directory)) {
            return findAnyFileFromDirectory(filename, walk)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            String.format("File %s was not found in directory", filename)));
        } catch (IOException e) {
            throw new InvalidFileException(
                    String.format("Something went wrong during searching file %s in the directory", filename));
        }
    }

    @Override
    public String getDataFromDirectoryByNameAndExtension(Path directory, String filename, String fileExtension) {
        try (Stream<Path> walk = Files.walk(directory)) {
            return walk
                    .map(Path::toFile)
                    .filter(item -> item.getName().equals(filename + checkFileExtension(fileExtension)))
                    .map(File::toPath)
                    .map(this::readDataFromFile)
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            throw new FileServiceException(
                    String.format("Something went wrong during retrieving data %s from the directory", filename));
        }
    }

    @Override
    public void deleteFile(Path file) {
        try {
            Files.delete(file);
        } catch (IOException e) {
            throw new FileServiceException(String.format("Failed to delete file with a name %s", file.getFileName()));
        }
    }

    @Override
    public Path copyFile(Path sourceFile, Path targetDirectory, String fileName) {
        log.info("Will copy file {} to {}", sourceFile, targetDirectory);
        checkDirectoryPath(targetDirectory);
        Path copiedFile = Optional.ofNullable(fileName)
                .map(targetDirectory::resolve)
                .orElse(targetDirectory.resolve(sourceFile.getFileName()));
        try {
            return Files.copy(sourceFile, copiedFile);
        } catch (IOException e) {
            throw new FileServiceException(String.format("Something went wrong during copying file %s.",
                                                         sourceFile.getFileName()));
        }
    }

    @Override
    public WorkloadInstanceHelmSourceUrl getInstance(Path directory) {
        byte[] jsonContent = getFileContentIfPresent(directory);
        return convertByteArrayToWorkloadInstance(jsonContent);
    }

    @Override
    public Optional<Path> getFileFromDirectoryByExtensionIfPresent(Path directory, String fileExtension) {
        checkFileExtension(fileExtension);
        log.info("Search file with {} extension in the directory: {}", fileExtension, directory.toAbsolutePath());
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(directory, String.format("*%s", fileExtension))) {
            return StreamSupport.stream(paths.spliterator(), false).findAny();
        } catch (IOException e) {
            throw new FileServiceException(String.format("Something went wrong during searching file by extension %s in the directory",
                    fileExtension));
        }
    }

    @Override
    public boolean fileExists(Path directory) {
        return Files.exists(directory);
    }

    @Override
    public String readStringFromPath(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException ex) {
            throw new InvalidFileException("Error when try to read path");
        }
    }

    @Override
    public Path copyFileWithReplaceExisting(Path pathToCopy, Path targetPath) {
        try {
            return Files.copy(pathToCopy, targetPath, REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new SecurityOperationException("Error when try to copy file");
        }
    }

    private WorkloadInstanceHelmSourceUrl convertByteArrayToWorkloadInstance(byte[] workloadInstanceByteArray) {
        var objectMapper = new ObjectMapper();
        WorkloadInstanceHelmSourceUrl workloadInstance;
        try {
            workloadInstance = objectMapper.readValue(workloadInstanceByteArray, WorkloadInstanceHelmSourceUrl.class);
        } catch (IOException e) {
            throw new InvalidFileException(String.format("Can't read workloadInstance. Details: %s", e.getMessage()));
        }
        return workloadInstance;
    }

    private void deleteFiles(Path directory) {
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(reverseOrder()).forEach(this::deleteFile);
        } catch (IOException e) {
            throw new FileServiceException("Failed to delete directory");
        }
    }

    private void deleteFilesIfExists(Path directory) {
        try (Stream<Path> walk = Files.walk(directory)) {
            walk.sorted(reverseOrder()).forEach(this::deleteFile);
        } catch (NoSuchFileException e) {
            log.info("Directory wasn't deleted because of absence");
        } catch (IOException e) {
            throw new FileServiceException("Failed to delete directory");
        }
    }

    private void checkDirectoryPath(Path targetDirectory) {
        if (!targetDirectory.toFile().isDirectory()) {
            throw new FileServiceException(String.format("You must specify a directory for the copy of the file. But %s is not a directory.",
                                                         targetDirectory.getFileName()));
        }
    }

    private String checkFileExtension(String fileExtension) {
        if (fileExtension.startsWith(DOT_DELIMITER)) {
            return fileExtension;
        } else {
            throw new FileServiceException(String.format("This is not file extension, please check %s", fileExtension));
        }
    }

    private Optional<Path> findAnyFileFromDirectory(final String filename, final Stream<Path> walk) {
        return walk
                .map(Path::toAbsolutePath)
                .filter(fileName -> fileName.getFileName().toString().equals(filename))
                .findAny();
    }
}
