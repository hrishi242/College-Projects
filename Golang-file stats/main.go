package main

import (
    "encoding/json"
    "fmt"
    "io/ioutil"
    "net/http"
    "os"
    "path/filepath"
    "sync"
)

type FileStats struct {
    Filename string `json:"filename"`
    Size     string `json:"size"`
}

type FileStatsResponse struct {
    Files         []FileStats `json:"files"`
    TotalSize     string      `json:"total_size"`
    AverageSize   string      `json:"average_size"`
    LargestFile   FileStats   `json:"largest_file"`
    SmallestFile  FileStats   `json:"smallest_file"`
}

func handleFileStats(w http.ResponseWriter, r *http.Request) {
    files, err := ioutil.ReadDir("static_files")
    if err != nil {
        fmt.Println("Error reading directory:", err)
        http.Error(w, "Error reading directory", http.StatusInternalServerError)
        return
    }

    var fileStats []FileStats
    var wg sync.WaitGroup
    var mu sync.Mutex

    for _, file := range files {
        if !file.IsDir() {
            // Open the file
            f, err := os.Open(filepath.Join("static_files", file.Name()))
            if err != nil {
                fmt.Println("Error opening file:", err)
                continue
            }

            // Read file content into memory
            content, err := ioutil.ReadAll(f)
            if err != nil {
                fmt.Println("Error reading file:", err)
                f.Close()
                continue
            }
            f.Close()

            // Launch a goroutine to process each file
            wg.Add(1)
            go func(filename string, content []byte) {
                defer wg.Done()

                size := int64(len(content))

                mu.Lock()
                fileStats = append(fileStats, FileStats{
                    Filename: filename,
                    Size:     formatFileSize(size),
                })
                mu.Unlock()
            }(file.Name(), content)
        }
    }

    // Wait for all goroutines to finish
    wg.Wait()

    // Calculate overall statistics
    var totalSize int64
    var largestFile, smallestFile *FileStats
    for i, stats := range fileStats {
        totalSize += stats.sizeInBytes()
        if largestFile == nil || stats.sizeInBytes() > largestFile.sizeInBytes() {
            largestFile = &fileStats[i]
        }
        if smallestFile == nil || stats.sizeInBytes() < smallestFile.sizeInBytes() {
            smallestFile = &fileStats[i]
        }
    }
    averageSize := float64(totalSize) / float64(len(fileStats))

    // Prepare the response
    response := FileStatsResponse{
        Files:         fileStats,
        TotalSize:     formatFileSize(totalSize),
        AverageSize:   formatFileSize(int64(averageSize)),
        LargestFile:   *largestFile,
        SmallestFile:  *smallestFile,
    }

    // Send results back to client
    w.Header().Set("Access-Control-Allow-Origin", "*")
    w.Header().Set("Content-Type", "application/json")
    json.NewEncoder(w).Encode(response)
}

func formatFileSize(size int64) string {
    units := []string{"B", "KB", "MB", "GB", "TB"}
    i := 0
    for size >= 1024 && i < len(units)-1 {
        size /= 1024
        i++
    }
    return fmt.Sprintf("%d %s", size, units[i])
}

func (fs FileStats) sizeInBytes() int64 {
    size, _ := parseFileSize(fs.Size)
    return size
}

func parseFileSize(size string) (int64, error) {
    var s int64
    var unit string
    _, err := fmt.Sscanf(size, "%d %s", &s, &unit)
    if err != nil {
        return 0, err
    }
    switch unit {
    case "B":
        return s, nil
    case "KB":
        return s * 1024, nil
    case "MB":
        return s * 1024 * 1024, nil
    case "GB":
        return s * 1024 * 1024 * 1024, nil
    case "TB":
        return s * 1024 * 1024 * 1024 * 1024, nil
    default:
        return 0, fmt.Errorf("unknown unit: %s", unit)
    }
}

func main() {
    http.HandleFunc("/filestats", handleFileStats)
    
    // Serve the React application
    http.Handle("/", http.FileServer(http.Dir("./my-file-size-analyzer/build")))
    
    fmt.Println("Server running on port 8080")
    http.ListenAndServe(":8080", nil)
}