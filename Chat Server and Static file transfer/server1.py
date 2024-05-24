import socket
import threading
import os
import time
from threading import Thread, Lock
from collections import deque
import ssl

# Define constants
HOST = '0.0.0.0'
PORTS = {
    "ann": 8001,
    "badminton": 8002,
    "cricket": 8003,
    "football": 8004,
    "kabaddi": 8005
}
FOLDERS = list(PORTS.keys())
NUM_FILES_PER_FOLDER = 10
NUM_PARTITIONS = 3  # Number of partitions per folder
ARTICLE_DELAY = 60  # 1 min delay between sending articles
CLIENT_DELAY = 10   # 10 sec delay between sending articles to multiple clients
END_TRANSMISSION_SIGNAL = "END_TRANSMISSION"
DISCONNECT_SIGNAL = "disconnect"

# Lock for thread-safe access to shared resources
lock = Lock()

# Initialize partitions
partitions = {folder_name: [deque() for _ in range(NUM_PARTITIONS)] for folder_name in FOLDERS}
available_files = {folder_name: [f"article{i}.txt" for i in range(1, NUM_FILES_PER_FOLDER + 1)] for folder_name in FOLDERS}
client_partitions = {}  # Dictionary to track client-partition mappings

# Distribute files to partitions
for folder_name in FOLDERS:
    for file_name in available_files[folder_name]:
        partition_idx = hash(file_name) % NUM_PARTITIONS
        partitions[folder_name][partition_idx].append(file_name)

# Function to send articles to clients
def send_articles(client_socket, folder_name, partition_idx):
    folder_path = os.path.join(os.getcwd(), folder_name)
    partition = partitions[folder_name][partition_idx]
    partition_line = '-' * 100 + f'\nPartition {partition_idx}\n' + '-' * 100 + '\n'  # Partition line
    client_socket.send(partition_line.encode())  # Send partition line

    while partition:
        file_name = partition.popleft()
        file_path = os.path.join(folder_path, file_name)
        with open(file_path, 'r', encoding='utf-8') as file:
            article_content = file.read()
        client_socket.send(article_content.encode())  # Send article content
        time.sleep(1)  # 1 sec spacing between sending articles

    print(f"All files sent to client for folder '{folder_name}', partition {partition_idx}")
    client_socket.send(END_TRANSMISSION_SIGNAL.encode())  # Send end of transmission signal

# Function to handle file client requests
def handle_file_client(client_socket, address, folder_name, partition_idx):
    with lock:
        print(f"Connection from {address} for folder {folder_name}, partition {partition_idx} has been established.")
        client_partitions[client_socket] = (folder_name, partition_idx)
        send_articles(client_socket, folder_name, partition_idx)

    while True:
        try:
            client_socket.settimeout(5)  # Set a timeout for receiving data from client
            data = client_socket.recv(4096).decode()
            if not data or data == DISCONNECT_SIGNAL:
                print(f"Client {address} disconnected from folder '{folder_name}'.")
                with lock:
                    folder_name, partition_idx = client_partitions.pop(client_socket)
                    partitions[folder_name][partition_idx] = deque(available_files[folder_name])
                    print(f"Partition {partition_idx} for folder '{folder_name}' is now available.")
                break
        except socket.timeout:
            continue
        except ConnectionAbortedError:
            print(f"Client {address} disconnected abruptly from folder '{folder_name}'.")
            with lock:
                folder_name, partition_idx = client_partitions.pop(client_socket)
                partitions[folder_name][partition_idx] = deque(available_files[folder_name])
                print(f"Partition {partition_idx} for folder '{folder_name}' is now available.")
            break
        except ConnectionResetError:
            print(f"Client {address} connection reset from folder '{folder_name}'.")
            with lock:
                folder_name, partition_idx = client_partitions.pop(client_socket)
                partitions[folder_name][partition_idx] = deque(available_files[folder_name])
                print(f"Partition {partition_idx} for folder '{folder_name}' is now available.")
            break
        except KeyboardInterrupt:
            print("Server is shutting down...")
            break

    client_socket.close()

# Dictionary to store usernames and passwords
user_credentials = {
    "user1": "password1",
    "user2": "password2",
    "user3": "password3",
    # Add more users as needed
}

active_users = {}

def listen_for_messages(client_socket, username):
    while True:
        try:
            message = client_socket.recv(2048).decode('utf-8')
            if message:
                if message == "!disconnect":
                    client_socket.sendall("You have been disconnected.".encode('utf-8'))
                    client_socket.close()
                    del active_users[username]
                    print(f"{username} has been disconnected.")
                    break
                elif ":" in message:
                    recipient, message_content = message.split(":", 1)
                    if recipient == "everyone":
                        for client in active_users.values():
                            if client != client_socket:
                                client.sendall(f"[{username}]: {message_content}".encode('utf-8'))
                    elif recipient in active_users:
                        recipient_socket = active_users[recipient]
                        recipient_socket.sendall(f"[{username}]: {message_content}".encode('utf-8'))
                    else:
                        client_socket.sendall("Recipient not found.".encode('utf-8'))
                else:
                    print(f"[{username}]: {message}")
                    for client in active_users.values():
                        if client != client_socket:
                            client.sendall(f"[{username}]: {message}".encode('utf-8'))
        except ConnectionResetError:
            del active_users[username]
            print(f"{username} has been disconnected.")
            break

def handle_chat_client(client_socket, address):
    username = client_socket.recv(2048).decode('utf-8')
    if username in user_credentials:
        if username in active_users:
            client_socket.sendall("User is already connected. Please disconnect first.".encode('utf-8'))
            client_socket.close()
        else:
            client_socket.sendall("Username recognized. Please enter your password:".encode('utf-8'))
            password = client_socket.recv(2048).decode('utf-8')
            if password == user_credentials[username]:
                client_socket.sendall("Authentication successful. You have joined the chat room.".encode('utf-8'))
                active_users[username] = client_socket  # Add user to active users
                threading.Thread(target=listen_for_messages, args=(client_socket, username)).start()
                print(f"{username} has joined the chat room.")
            else:
                client_socket.sendall("Invalid password. Connection closed.".encode('utf-8'))
                client_socket.close()
    else:
        client_socket.sendall("Username not recognized. Connection closed.".encode('utf-8'))
        client_socket.close()

def folder_server(folder_name, port):
    ssl_context = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    ssl_context.load_cert_chain(certfile='cert.pem', keyfile='key.pem')

    server_socket = ssl_context.wrap_socket(socket.socket(socket.AF_INET, socket.SOCK_STREAM), server_side=True)
    server_socket.bind((HOST, port))
    server_socket.listen(5)

    print(f"Server for folder '{folder_name}' listening on {HOST}:{port}...")

    while True:
        try:
            client_socket, address = server_socket.accept()
            partition_idx = int(client_socket.recv(4096).decode())
            client_thread = Thread(target=handle_file_client, args=(client_socket, address, folder_name, partition_idx))
            client_thread.start()
        except KeyboardInterrupt:
            print("Server is shutting down...")
            break

MAIN_SERVER_PORT = 1234

main_server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
main_server_socket.bind((HOST, MAIN_SERVER_PORT))
main_server_socket.listen()

print(f"Server is listening on {HOST}:{MAIN_SERVER_PORT}")

def main(folder_name, port, main_server_socket):
    # Start the folder server for the given folder and port
    folder_server_thread = threading.Thread(target=folder_server, args=(folder_name, port))
    folder_server_thread.start()

    print("All folder servers have started.")

    while True:
        client_socket, address = main_server_socket.accept()
        print(f"Connection established with {address}")
        choice = client_socket.recv(1024).decode()  # Receive client's choice
        print("Choice received:", choice)

        if choice == "1":
            folder_choice = client_socket.recv(1024).decode()  # Receive folder choice
            print("Folder choice received:", folder_choice)
            if folder_choice in PORTS:
                partition_idx_str = client_socket.recv(1024).decode()  # Receive partition index
                partition_idx = int(partition_idx_str)
                print("Partition index received:", partition_idx)
                threading.Thread(target=handle_file_client, args=(client_socket, address, folder_choice, partition_idx)).start()
            else:
                print(f"Invalid folder choice '{folder_choice}' received from client.")
                client_socket.close()

        elif choice == "2":
            threading.Thread(target=handle_chat_client, args=(client_socket, address)).start()
        else:
            print("Invalid choice received from client.")
            client_socket.close()

if __name__ == "__main__":
    for folder_name, port in PORTS.items():
        main_thread = threading.Thread(target=main, args=(folder_name, port, main_server_socket))
        main_thread.start()
