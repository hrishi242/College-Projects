import socket
import threading
import signal
import sys
import os

# Signal handler for graceful shutdown
def signal_handler(sig, frame):
    print("\n[SERVER SHUTDOWN] Signal received, closing server socket...")
    server_socket.close()
    sys.exit(0)

# Attach the signal handler to SIGINT
signal.signal(signal.SIGINT, signal_handler)

# Authentication function
def authenticate_client(client_socket):
    client_socket.send("Handshake".encode('utf-8'))
    username = client_socket.recv(1024).decode('utf-8')
    password = client_socket.recv(1024).decode('utf-8')

    # Check the credentials
    users = dict()
    with open('id_passwd.txt', 'r') as file:
        for line in file.readlines():
            users[line.split(':')[0]] = line.split(':')[1].strip()
    if username in users and users[username] == password:
        client_socket.send("Authentication successful.".encode('utf-8'))
        return username
    else:
        client_socket.send("Authentication failed.".encode('utf-8'))
        return None

# Handle client requests
def handle_client(client_socket, addr):
    print(f"[NEW CONNECTION] {addr} connected.")
    username = authenticate_client(client_socket)

    if not username:
        print(f"[DISCONNECTED] Authentication failed for {addr}")
        client_socket.close()
        return

    user_directory = f"server_storage/{username}"
    if not os.path.exists(user_directory):
        os.makedirs(user_directory)

    while True:
        data = client_socket.recv(1024).decode('utf-8')

        if not data:
            break

        # Handle the file upload request
        if data.startswith("UPLOAD:"):
            file_name = data[7:]
            client_socket.send("READY".encode('utf-8'))

            with open(f"{user_directory}/{file_name}", 'wb') as f:
                while True:
                    chunk = client_socket.recv(1024)
                    if chunk == b"DONE":
                        break
                    f.write(chunk)
            client_socket.send(f"File '{file_name}' uploaded successfully.".encode('utf-8'))

        # Handle the file download request
        elif data.startswith("DOWNLOAD:"):
            file_name = data[9:]
            file_path = f"{user_directory}/{file_name}"
            if os.path.exists(file_path):
                client_socket.send("READY".encode('utf-8'))
                
                with open(file_path, 'rb') as f:
                    while (chunk := f.read(1024)):
                        client_socket.send(chunk)
                client_socket.send(b"DONE")  # End-of-file marker
            else:
                client_socket.send(f"ERROR: File '{file_name}' not found.".encode('utf-8'))

        # Handle the file view request
        elif data.startswith("VIEW:"):
            file_name = data[5:]
            file_path = f"{user_directory}/{file_name}"
            if os.path.exists(file_path):
                try:
                    with open(file_path, 'r') as f:
                        file_content = f.read(1024)
                    client_socket.send(f"File preview:\n{file_content}".encode('utf-8'))
                except UnicodeDecodeError:
                    client_socket.send("ERROR: File is not viewable as plain text.".encode('utf-8'))
            else:
                client_socket.send(f"ERROR: File '{file_name}' not found.".encode('utf-8'))

        # Handle the delete file request
        elif data.startswith("DELETE:"):
            file_name = data[7:]
            file_path = f"{user_directory}/{file_name}"
            if os.path.exists(file_path):
                os.remove(file_path)
                client_socket.send(f"File '{file_name}' deleted successfully.".encode('utf-8'))
            else:
                client_socket.send(f"ERROR: File '{file_name}' not found.".encode('utf-8'))

        # Handle the list files request
        elif data == "LIST":
            files = os.listdir(user_directory)
            if files:
                client_socket.send("\n".join(files).encode('utf-8'))
            else:
                client_socket.send("No files found.".encode('utf-8'))

        # Handle session exit
        elif data == "EXIT":
            client_socket.send("Session closed.".encode('utf-8'))
            break

        else:
            client_socket.send("Invalid command.".encode('utf-8'))

    client_socket.close()
    print(f"[DISCONNECTED] {addr} disconnected.")

# Start the server
def start_server():
    global server_socket
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(('0.0.0.0', 12345))
    server_socket.listen()
    print("[SERVER STARTED] Listening on port 12345")

    while True:
        try:
            client_socket, addr = server_socket.accept()
            client_thread = threading.Thread(target=handle_client, args=(client_socket, addr))
            client_thread.start()

            # Optional: Keep track of threads
            print(f"[ACTIVE CONNECTIONS] {threading.active_count() - 1}")
        except OSError:
            break  # Exit loop if server socket is closed

if __name__ == "__main__":
    start_server()
