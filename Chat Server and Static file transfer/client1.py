import socket
import threading
import ssl

# Define constants
SERVER_HOST = '127.0.0.1'
PORT = 1234
PORTS = {
    "ann": 8001,
    "badminton": 8002,
    "cricket": 8003,
    "football": 8004,
    "kabaddi": 8005
}
END_TRANSMISSION_SIGNAL = "END_TRANSMISSION"

# Function to receive server messages
def receive_messages(client_socket):
    while True:
        try:
            response = client_socket.recv(4096).decode()
            if response == END_TRANSMISSION_SIGNAL:
                print("End of transmission received.")
                break
            if not response:
                print("Server disconnected.")
                break
            print(response)
        except ConnectionAbortedError:
            print("Server disconnected unexpectedly.")
            break
        except KeyboardInterrupt:
            print("Client interrupted.")
            break

# Main function
def main():
    ssl_context = ssl.create_default_context()

    while True:
        try:
            choice = input("Enter 1 for file transfer or 2 for chat (or 'disconnect' to quit): ")
            if choice == "1":
                with ssl_context.wrap_socket(socket.socket(socket.AF_INET, socket.SOCK_STREAM), server_hostname=SERVER_HOST) as client_socket:
                    try:
                        client_socket.connect((SERVER_HOST, PORT))
                        client_socket.send(choice.encode())  # Send choice to the server

                        folder_name = input("Enter folder name (or 'disconnect' to quit): ")
                        if folder_name == 'disconnect':
                            break
                        if folder_name not in PORTS:
                            print("Invalid folder name.")
                            continue
                        client_socket.send(folder_name.encode())  # Send folder name to the server

                        partition_idx = int(input("Enter partition index (0-2): "))
                        if partition_idx < 0 or partition_idx > 2:
                            print("Invalid partition index.")
                            continue
                        client_socket.send(str(partition_idx).encode())  # Send partition index to the server

                        # Start a thread to receive server messages
                        receive_thread = threading.Thread(target=receive_messages, args=(client_socket,))
                        receive_thread.start()
                        receive_thread.join()  # Wait for the receive thread to finish
                    except ConnectionRefusedError:
                        print(f"Connection to server for folder '{folder_name}' refused.")
                        continue
                    except KeyboardInterrupt:
                        print("Client interrupted.")
                        break
                    finally:
                        # Inform the server about disconnection
                        client_socket.send("disconnect".encode())
            elif choice == "2":
                with ssl_context.wrap_socket(socket.socket(socket.AF_INET, socket.SOCK_STREAM), server_hostname=SERVER_HOST) as client_socket:
                    try:
                        client_socket.connect((SERVER_HOST, PORT))
                        client_socket.send(choice.encode())  # Send choice to the server

                        username = input("Enter your username: ").strip()
                        client_socket.sendall(username.encode('utf-8'))

                        response = client_socket.recv(2048).decode('utf-8')
                        print(response)

                        if "recognized" in response:
                            password = input("Enter your password: ").strip()
                            client_socket.sendall(password.encode('utf-8'))

                            authentication_response = client_socket.recv(2048).decode('utf-8')
                            print(authentication_response)

                            if "successful" in authentication_response:
                                receive_thread = threading.Thread(target=receive_messages, args=(client_socket,))
                                receive_thread.start()

                                print("You have joined the chat room. Type !disconnect to leave.")
                                while True:
                                    recipient = input("Enter the recipient's username (or type 'everyone' to send to all): ").strip()
                                    message = input("Enter your message: ").strip()
                                    if recipient.lower() == "everyone":
                                        recipient = "everyone"
                                    # Send both recipient and message content
                                    client_socket.sendall(f"{recipient}:{message}".encode('utf-8'))

                                    if message == "!disconnect":
                                        client_socket.sendall("!disconnect".encode('utf-8'))
                                        break
                            else:
                                print("Authentication failed. Connection closed.")
                                client_socket.close()
                                return
                        else:
                            print("Username not recognized. Connection closed.")
                            client_socket.close()
                            return
                    except ConnectionRefusedError:
                        print("Connection to server refused.")
                        continue
                    except KeyboardInterrupt:
                        print("Client interrupted.")
                        break
            else:
                print("Invalid choice.")
        except KeyboardInterrupt:
            print("Client interrupted.")
            break

if __name__ == "__main__":
    main()