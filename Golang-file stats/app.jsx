import { useState } from "react";

function App() {
  const [files, setFiles] = useState([]);
  const [result, setResult] = useState(null);

  const handleFileChange = (event) => {
    const selectedFiles = [...event.target.files];
    setFiles([...files, ...selectedFiles]);
  };

  const removeFile = (index) => {
    const newFiles = [...files];
    newFiles.splice(index, 1);
    setFiles(newFiles);
  };

  const addFileInput = () => {
    const input = document.createElement("input");
    input.type = "file";
    input.multiple = true;
    input.addEventListener("change", handleFileChange);
    input.click();
  };

  const submitFiles = () => {
    if (files.length === 0) {
      alert("Please select at least one file.");
      return;
    }

    const formData = new FormData();
    files.forEach((file) => {
      formData.append("files", file);
    });

    fetch("http://localhost:8080/filestats", {
      method: "POST",
      body: formData,
    })
      .then((response) => response.json())
      .then((data) => {
        setResult(data);
      });
  };

  return (
    <div className="w-full h-screen flex flex-col items-center bg-gradient-to-b from-[#d9a7c7] to-[#fffcdc]">
      <h1 className="text-4xl font-semibold pb-6 md:pb-10 pt-4 underline">
        File Size Analyzer
      </h1>
      {files.length > 0 && (
        <div className="w-full max-w-[350px] flex flex-col justify-center m-4 ">
          <div className="w-full mt-2 max-w-[350px] border border-blue-300 py-2 pl-4 pr-2  bg-blue-100 ">
            {files.map((file, index) => (
              <div key={index} className="py-2 flex justify-between items-center">
                <span className="px-2">{file.name}</span>
                <button onClick={() => removeFile(index)} className="">
                  Remove
                </button>
              </div>
            ))}
          </div>
        </div>
      )}
      <div className="w-full mt-4 p-4 max-w-[350px] flex justify-between items-center">
        <button
          onClick={addFileInput}
          className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-700 transition ease-in-out"
        >
          Add File
        </button>
        <button
          onClick={submitFiles}
          className="px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-700 transition ease-in-out"
        >
          Analyze
        </button>
      </div>

      {result && (
        <div className="fixed top-0 left-0 w-full h-full bg-black bg-opacity-50 flex justify-center items-center">
          <div className="w-full max-w-[350px] bg-white py-8 px-16 rounded-lg">
            <h2 className="text-2xl font-semibold text-center mb-6">
              File Statistics
            </h2>
            <div className="w-full flex flex-col justify-center items-center">
              <p className="text-lg mb-2">Total Size: {result.total_size}</p>
              <p className="text-lg mb-2">Average Size: {result.average_size}</p>
              <p className="text-lg mb-4">
                Largest File: {result.largest_file.filename} ({
                  result.largest_file.size
                })
              </p>
              <p className="text-lg mb-4">
                Smallest File: {result.smallest_file.filename} ({
                  result.smallest_file.size
                })
              </p>
              <h3 className="text-lg font-semibold mb-2">Individual Files:</h3>
              <div className="w-full flex flex-col items-start">
                {result.files.map((file, index) => (
                  <p key={index} className="text-lg mb-1">
                    {file.filename} ({file.size})
                  </p>
                ))}
              </div>
            </div>
            <div className="flex justify-center items-center">
              <button
                onClick={() => setResult(null)}
                className="px-4 py-2 bg-blue-500 hover:bg-blue-700 text-white rounded-lg mt-4"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default App;