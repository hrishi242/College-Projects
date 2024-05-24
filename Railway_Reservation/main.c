#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <time.h>

#define MAX_LINE_LENGTH 256
#define MAX_STATION_LENGTH 50
#define MAX_TRAIN_LENGTH 20
/*
#define MAX_SEATS_UNRESERVED 200
#define MAX_SEATS_GENERAL 500
#define MAX_SEATS_AC 200
#define MAX_SEATS_SLEEPER 100
 */
#define MAX_TICKETS 100
#define MAX_COACH_TYPE_LENGTH 20

struct Train {
    char number[MAX_TRAIN_LENGTH];
    char name[MAX_TRAIN_LENGTH];
    char stations[MAX_STATION_LENGTH][MAX_STATION_LENGTH];
    int station_numbers[MAX_STATION_LENGTH];
    int station_count;
    int seats_unreserved;
    int seats_general;
    int seats_ac;
    int seats_sleeper;
};

struct Passenger {
    char name[MAX_STATION_LENGTH];
    int age;
    char date[MAX_STATION_LENGTH];
};

struct Ticket {
    int serial_number;  // New field for the serial number
    char name[MAX_STATION_LENGTH];
    int age;
    char date[MAX_STATION_LENGTH];
    char train_number[MAX_TRAIN_LENGTH];
    char train_name[MAX_TRAIN_LENGTH];
    char coach_type[MAX_COACH_TYPE_LENGTH];
    char source_station[MAX_STATION_LENGTH];
    char destination_station[MAX_STATION_LENGTH];
};

int nextSerialNumber = 1;

struct Ticket tickets[MAX_TICKETS];
struct Train trains[100]; // Assuming a maximum of 100 trains
int cchoice;
int ticket_count = 0;

int stricmp(const char *a, const char *b) {
    while (*a && *b) {
        if (tolower((unsigned char)*a) != tolower((unsigned char)*b)) {
            return 1;
        }
        a++;
        b++;
    }
    return *a || *b;
}

char* trim(char* str) {
    while (isspace((unsigned char)*str)) str++;
    if (*str) {
        char* end = str + strlen(str) - 1;
        while (end > str && isspace((unsigned char)*end)) end--;
        end[1] = '\0';
    }
    return str;
}

int parseDate(const char *dateStr, struct tm *dateStruct) {
    // Parse the date string in the format "YYYY-MM-DD"
    if (sscanf(dateStr, "%d-%d-%d", &dateStruct->tm_year, &dateStruct->tm_mon, &dateStruct->tm_mday) != 3) {
        return 0;  // Failed to parse date
    }

    // Adjust the year and month values (tm_year is the number of years since 1900, and tm_mon is 0-based)
    dateStruct->tm_year -= 1900;
    dateStruct->tm_mon -= 1;

    // Set the time components to midnight
    dateStruct->tm_hour = 0;
    dateStruct->tm_min = 0;
    dateStruct->tm_sec = 0;

    // Normalize the time structure
    if (mktime(dateStruct) == -1) {
        return 0;  // Failed to normalize date
    }

    return 1;  // Date parsed successfully
}

void writeTicketToCSV(FILE *file, struct Ticket ticket, struct Passenger passenger, struct Train train,
                      const char *source, const char *destination, int seatNumber) {
    fprintf(file, "%d,%s,%s,%d,%s,%s,%s,%s,%s,%d\n",
            ticket.serial_number, passenger.name, passenger.date, passenger.age,
            source, destination, ticket.coach_type, train.number, train.name, seatNumber);
}

void removeTicketFromWaitingListFile(const char* waitingListFileName, const char* ticketDetails) {
    FILE *waitingListFile = fopen(waitingListFileName, "r");
    if (waitingListFile == NULL) {
        perror("Failed to open the waiting list file");
        return;
    }

    FILE *tempFile = fopen("temp_waiting_list.csv", "w");
    if (tempFile == NULL) {
        perror("Failed to create a temporary file");
        fclose(waitingListFile);
        return;
    }

    char line[MAX_LINE_LENGTH];
    int ticketRemoved = 0;

    // Read the file line by line and copy to the temporary file, excluding the specified ticket details
    while (fgets(line, sizeof(line), waitingListFile) != NULL) {
        // Check if the current line contains the ticket details to be removed
        if (strstr(line, ticketDetails) == NULL) {
            // If the line doesn't contain the ticket details, write it to the temporary file
            fprintf(tempFile, "%s", line);
        } else {
            ticketRemoved = 1; // Mark that the ticket was found and removed
        }
    }

    fclose(waitingListFile);
    fclose(tempFile);

    if (!ticketRemoved) {
        printf("Ticket not found in the waiting list.\n");
        remove("temp_waiting_list.csv"); // Remove the temporary file if the ticket wasn't found
        return;
    }

    // Reopen the waiting list file in write mode to overwrite it with the updated content
    waitingListFile = fopen(waitingListFileName, "w");
    if (waitingListFile == NULL) {
        perror("Failed to reopen the waiting list file");
        return;
    }

    // Reopen the temporary file to copy its content back to the waiting list file
    tempFile = fopen("temp_waiting_list.csv", "r");
    if (tempFile == NULL) {
        perror("Failed to open the temporary file");
        fclose(waitingListFile);
        return;
    }

    // Copy the content of the temporary file back to the waiting list file
    while (fgets(line, sizeof(line), tempFile) != NULL) {
        fprintf(waitingListFile, "%s", line);
    }

    fclose(waitingListFile);
    fclose(tempFile);

    // Remove the temporary file after successful transfer
    remove("temp_waiting_list.csv");
}

void displayTrainInfo(struct Train train) {
    printf("Train Number: %s\n", train.number);
    printf("Train Name: %s\n", train.name);
    printf("Stations:\n");
    for (int i = 0; i < train.station_count; i++) {
        printf("- %s\n", train.stations[i]);
    }
    printf("\n");
}

// Function to check if the date is valid within one month from today
int isDateValid(struct tm *travelDate) {
    time_t currentTime = time(NULL);
    struct tm *currentDate = localtime(&currentTime);

    return (difftime(mktime(travelDate), currentTime) >= 0 &&
            difftime(mktime(travelDate), currentTime + 30 * 24 * 60 * 60) <= 0);
}

void moveFromWaitingListToBookedTickets(struct Train train) {
    FILE *waitingListFile = fopen("waiting_list.csv", "r");
    if (waitingListFile == NULL) {
        perror("Failed to open the waiting list file");
        return;
    }

    struct Ticket waitingListTicket;
    int seatNumber;

    // Read the first ticket from the waiting list
    if (fscanf(waitingListFile, "%d,%49[^,],%19[^,],%d,%19[^,],%19[^,],%19[^,],%19[^,],%19[^,],%d\n",
               &waitingListTicket.serial_number, waitingListTicket.name, waitingListTicket.date, &waitingListTicket.age,
               waitingListTicket.source_station, waitingListTicket.destination_station, waitingListTicket.coach_type,
               waitingListTicket.train_number, waitingListTicket.train_name, &seatNumber) != 10) {
        printf("No tickets in the waiting list.\n");
        fclose(waitingListFile);
        return;
    }

    fclose(waitingListFile);

    // Check if the waiting list ticket is already present in booked tickets
    FILE *bookedTicketsFile = fopen("booked_tickets.csv", "r");
    if (bookedTicketsFile == NULL) {
        perror("Failed to open the booked tickets file");
        return;
    }

    int ticketExistsInBooked = 0;
    struct Ticket bookedTicket;

    while (fscanf(bookedTicketsFile, "%d,%49[^,],%19[^,],%d,%19[^,],%19[^,],%19[^,],%19[^,],%19[^,],%d\n",
                  &bookedTicket.serial_number, bookedTicket.name, bookedTicket.date, &bookedTicket.age,
                  bookedTicket.source_station, bookedTicket.destination_station, bookedTicket.coach_type,
                  bookedTicket.train_number, bookedTicket.train_name, &seatNumber) == 10) {
        if (bookedTicket.serial_number == waitingListTicket.serial_number) {
            ticketExistsInBooked = 1;
            break;
        }
    }

    fclose(bookedTicketsFile);

    if (!ticketExistsInBooked) {
        // Move the ticket from waiting list to booked tickets
        FILE *bookedTicketsFileAppend = fopen("booked_tickets.csv", "a");
        if (bookedTicketsFileAppend == NULL) {
            perror("Failed to open the booked tickets file");
            return;
        }

        fprintf(bookedTicketsFileAppend, "%d,%s,%s,%d,%s,%s,%s,%s,%s,%d\n",
                waitingListTicket.serial_number, waitingListTicket.name, waitingListTicket.date, waitingListTicket.age,
                waitingListTicket.source_station, waitingListTicket.destination_station, waitingListTicket.coach_type,
                waitingListTicket.train_number, waitingListTicket.train_name, seatNumber);

        fclose(bookedTicketsFileAppend);

        printf("Ticket moved from waiting list to booked tickets.\n");
        int ticketMovedSuccessfully=1;
        struct Ticket ticket;
        // Generating a string based on ticket details to use as the parameter for removal
        char ticketDetails[MAX_LINE_LENGTH];
        sprintf(ticketDetails, "%s,%d,%s", ticket.name, ticket.age, ticket.date);

        if (ticketMovedSuccessfully) {
            // Call the function to remove the ticket from the waiting list file
            removeTicketFromWaitingListFile("waiting_list.csv", ticketDetails);
        }

    } else {
        printf("Ticket already exists in booked tickets. Not moved from waiting list.\n");
    }
}

void displayTicket(struct Passenger passenger, struct Train train, struct Ticket ticket, const char* source, const char* destination) {
    printf("\nTicket Details:\n");
    printf("Serial Number: %d\n", ticket.serial_number);
    printf("Name: %s\n", passenger.name);
    printf("Age: %d\n", passenger.age);
    printf("Date of Travel: %s\n", passenger.date);
    printf("Train Number: %s\n", train.number);
    printf("Train Name: %s\n", train.name);
    printf("Source Station: %s\n", source);  // Display the user-entered source station
    printf("Destination Station: %s\n", destination);  // Display the user-entered destination station
    printf("Coach Type: %s\n", ticket.coach_type);
}

void storeTicket(struct Ticket *ticket, struct Passenger passenger, struct Train train, struct tm *travelDate,
                 const char *source, const char *destination, int st_index) {
    char coachType[MAX_COACH_TYPE_LENGTH];

    // Input: Coach Type
    printf("Confirm coach type (1.Unreserved/2.General/3.AC/4.Sleeper): ");
    scanf("%s", coachType);

    // Validate and set the coach type
    if (!(stricmp(coachType, "Unreserved") == 0 || stricmp(coachType, "General") == 0 ||
          stricmp(coachType, "AC") == 0 || stricmp(coachType, "Sleeper") == 0)) {
        printf("Invalid coach type. Please choose from Unreserved, General, AC, or Sleeper.\n");
        return;
    }

    // Generate a random seat number within the available seats for the chosen coach type
    int seatNumber = -1;
    //scanf("%d",&choice);
    switch (cchoice) {
        case 1:
            if (train.seats_unreserved > 0) {
                seatNumber = rand() % train.seats_unreserved + 1;
                printf("Seat booked in Unreserved Coach. Seat Number: %d\n", seatNumber);
                trains[st_index].seats_unreserved--;
            } else {
                printf("No available seats in Unreserved Coach.\n");
            }
            break;
        case 2:
            if (train.seats_general > 0) {
                seatNumber = rand() % train.seats_general + 1;
                printf("Seat booked in General Coach. Seat Number: %d\n", seatNumber);
                trains[st_index].seats_general--;
            } else {
                printf("No available seats in General Coach.\n");
            }
            break;
        case 3:
            if (train.seats_ac > 0) {
                seatNumber = rand() % train.seats_ac + 1;
                printf("Seat booked in AC Coach. Seat Number: %d\n", seatNumber);
                trains[st_index].seats_ac--;
            } else {
                printf("No available seats in AC Coach.\n");
            }
            break;
        case 4:
            if (train.seats_sleeper > 0) {
                seatNumber = rand() % train.seats_sleeper + 1;
                printf("Seat booked in Sleeper Coach. Seat Number: %d\n", seatNumber);
                trains[st_index].seats_sleeper--;
            } else {
                printf("No available seats in Sleeper Coach.\n");
            }
            break;
        default:
            printf("Invalid choice.\n");
    }

    // Check if a seat number was successfully assigned
    if (seatNumber != -1) {
        // Set the serial number, coach type, and other details in the ticket
        ticket->serial_number = nextSerialNumber++;
        strcpy(ticket->name, passenger.name);
        ticket->age = passenger.age;
        strcpy(ticket->date, passenger.date);
        strcpy(ticket->train_number, train.number);
        strcpy(ticket->train_name, train.name);
        strcpy(ticket->coach_type, coachType);

        // Add source and destination station information
        strcpy(ticket->source_station, source);
        strcpy(ticket->destination_station, destination);

        // Display the serial number, coach type, and other details
        printf("Serial Number: %d\n", ticket->serial_number);
        printf("Coach Type: %s\n", ticket->coach_type);
        printf("Seat Number: %d\n", seatNumber);

        // Display the ticket details
        displayTicket(passenger, train, *ticket, source, destination);

        // Open the CSV file in append mode
        FILE *csvFile = fopen("C:\\Users\\venka\\CLionProjects\\untitled\\cmake-build-debug\\booked_tickets.csv", "a");

        // Check if the file is opened successfully
        if (csvFile == NULL) {
            perror("Failed to open the CSV file");
            return;
        }

        // Write the current ticket to the CSV file
        writeTicketToCSV(csvFile, *ticket, passenger, train, source, destination, seatNumber);

        // Close the CSV file
        fclose(csvFile);

        // Increment the ticket_count after successfully storing a ticket
        ticket_count++;
    }
    else {
        // The coach is full; move the current ticket to the waiting list
        FILE *waitingListFile = fopen("C:\\Users\\venka\\CLionProjects\\untitled\\cmake-build-debug\\waiting_list.csv", "a");
        if (waitingListFile == NULL) {
            perror("Failed to open the waiting list file");
            return;
        }

        // Write the details of the ticket that couldn't be booked to the waiting list file
        fprintf(waitingListFile, "%s,%s,%s,%d,%s,%s,%s,%s\n",
                passenger.name, passenger.date, coachType, passenger.age,
                source, destination, train.number, train.name);

        fclose(waitingListFile);

        printf("Coach is full. Ticket added to the waiting list.\n");
    }
}


int readNextSerialNumber() {
    FILE *file = fopen("C:\\Users\\venka\\CLionProjects\\untitled\\cmake-build-debug\\serial_number.txt", "r");
    if (file != NULL) {
        int serialNumber;
        if (fscanf(file, "%d", &serialNumber) == 1) {
            fclose(file);
            return serialNumber;
        } else {
            printf("Error reading serial number from file.\n");
        }
    } else {
        printf("Error opening serial_number.txt file.\n");
    }
    return 1;  // Default to 1 if there is an issue reading from the file
}


void writeNextSerialNumber(int serialNumber) {
    FILE *file = fopen("C:\\Users\\venka\\CLionProjects\\untitled\\cmake-build-debug\\serial_number.txt", "w");
    if (file != NULL) {
        fprintf(file, "%d", serialNumber);
        fclose(file);
    }
}

void removeTicketFromCSV(const char *csvFileName, int serialNumber,struct Train train, const char *passengerName, int choice) {
    // Create a temporary file to store the updated content
    FILE *tempFile = fopen("temp_tickets.csv", "w");
    if (tempFile == NULL) {
        perror("Failed to create a temporary file");
        return;
    }

    FILE *csvFile = fopen(csvFileName, "r");
    if (csvFile == NULL) {
        perror("Failed to open the CSV file");
        fclose(tempFile);
        return;
    }

    char line[MAX_LINE_LENGTH];
    int ticketRemoved = 0;
    int seatNumber = -1;
    char coachType[MAX_COACH_TYPE_LENGTH];

    // Read the file line by line and copy to the temporary file, excluding the specified serial number and passenger name
    while (fgets(line, sizeof(line), csvFile) != NULL) {
        int currentSerialNumber;
        char currentPassengerName[MAX_STATION_LENGTH];
        char currentCoachType[MAX_COACH_TYPE_LENGTH];
        char source[MAX_STATION_LENGTH];
        char destination[MAX_STATION_LENGTH];
        char trainNumber[MAX_TRAIN_LENGTH];
        char trainName[MAX_TRAIN_LENGTH];

        // Parse the seat number and coach type as well
        if (sscanf(line, "%d,%49[^,],%19[^,],%d,%19[^,],%19[^,],%19[^,],%19[^,],%19[^\n]", &currentSerialNumber,
                   currentPassengerName, currentCoachType, &seatNumber, source, destination, coachType, trainNumber, trainName) != 9) {
            fprintf(tempFile, "%s", line);
        } else if (currentSerialNumber != serialNumber || stricmp(trim(currentPassengerName), trim(passengerName)) != 0) {
            fprintf(tempFile, "%s", line);
        } else {
            // Mark that a ticket was removed
            ticketRemoved = 1;
            printf("Ticket with serial number %d and passenger name %s removed successfully.\n", serialNumber, passengerName);
            // Make the seat available again
            switch (choice) {
                case 1:
                    train.seats_unreserved++;
                    break;
                case 2:
                    train.seats_general++;
                    break;
                case 3:
                    train.seats_ac++;
                    break;
                case 4:
                    train.seats_sleeper++;
                    break;
                default:
                    printf("No such coach type");
                    break;
            }
        }
    }

    fclose(csvFile);
    fclose(tempFile);

    // Remove the original CSV file
    if (remove(csvFileName) != 0) {
        perror("Error deleting the original CSV file");
        return;
    }

    // Rename the temporary file to the original CSV file
    if (rename("temp_tickets.csv", csvFileName) != 0) {
        perror("Error renaming the temporary file");
        return;
    }

    if (!ticketRemoved) {
        printf("Ticket with serial number %d and passenger name %s not found.\n", serialNumber, passengerName);
    }
}


int main() {
    FILE *file = fopen("C:\\Users\\venka\\CLionProjects\\untitled\\trs.csv", "r");
    if (file == NULL) {
        perror("Failed to open the file");
        return 1;
    }

    char line[MAX_LINE_LENGTH];
    int train_count = 0;

    // Read the file line by line
    while (fgets(line, sizeof(line), file) != NULL) {
        char train_number[MAX_TRAIN_LENGTH];
        char train_name[MAX_TRAIN_LENGTH];
        char station[MAX_STATION_LENGTH];
        int station_number;
        int seats_unreserved, seats_general, seats_ac, seats_sleeper;

        sscanf(line, "%19[^,],%19[^,],%49[^,],%d,%d,%d,%d,%d[^\n]", train_number, train_name, station, &station_number,
               &seats_unreserved, &seats_general, &seats_ac, &seats_sleeper);

        // Trim leading and trailing whitespaces
        strcpy(train_number, trim(train_number));
        strcpy(train_name, trim(train_name));
        strcpy(station, trim(station));

        int existing_train_index = -1;

        // Check if the train number already exists in the array
        for (int i = 0; i < train_count; i++) {
            if (stricmp(trains[i].number, train_number) == 0) {
                existing_train_index = i;
                break;
            }
        }

        if (existing_train_index == -1) {
            // This is a new train, so initialize a new entry in the array
            strcpy(trains[train_count].number, train_number);
            strcpy(trains[train_count].name, train_name);
            trains[train_count].station_count = 0;
            trains[train_count].seats_unreserved = seats_unreserved;
            trains[train_count].seats_general = seats_general;
            trains[train_count].seats_ac = seats_ac;
            trains[train_count].seats_sleeper = seats_sleeper;
            existing_train_index = train_count;
            train_count++;
        }

        // Add the station and its corresponding number to the existing train's station list
        strcpy(trains[existing_train_index].stations[trains[existing_train_index].station_count], station);
        trains[existing_train_index].station_numbers[trains[existing_train_index].station_count] = station_number;
        trains[existing_train_index].station_count++;
    }

    fclose(file);

    // Now you have the train information organized by train number, their stations, station numbers, and available seats
    // Read the last serial number from the CSV file
    nextSerialNumber = readNextSerialNumber();
    if (nextSerialNumber == 0) {
        nextSerialNumber = 1; // Initialize to 1 if there was an issue reading from the file
    }

    int continueBooking = 1;
    while (continueBooking) {
        int choice;
        printf("Choose an option:\n");
        printf("1. Book a Ticket\n");
        printf("2. Cancel a Ticket\n");
        printf("Enter your choice (1 or 2): ");
        scanf("%d", &choice);

        switch (choice) {
            case 1: {
                int bookAnotherTicket = 1;

                while (bookAnotherTicket) {
                    // Input: Source and Destination Stations
                    char source[MAX_STATION_LENGTH];
                    char destination[MAX_STATION_LENGTH];
                    // Input: Source and Destination Stations
                    printf("Enter the source station: ");
                    scanf("%s", source);
                    printf("Enter the destination station: ");
                    scanf("%s", destination);

                    // Find the trains that go through the source and destination stations
                    printf("Available Trains:\n");
                    int found_trains_count = 0;
                    for (int i = 0; i < train_count; i++) {
                        int source_index = -1;
                        int destination_index = -1;

                        // Check if the source and destination stations exist in the current train's station list
                        for (int j = 0; j < trains[i].station_count; j++) {
                            if (stricmp(trains[i].stations[j], source) == 0) {
                                source_index = j;
                            }
                            if (stricmp(trains[i].stations[j], destination) == 0) {
                                destination_index = j;
                            }
                        }

                        // Check if both source and destination stations exist in the same train
                        if (source_index != -1 && destination_index != -1 && source_index < destination_index) {
                            displayTrainInfo(trains[i]);
                            found_trains_count++;
                        }
                    }

                    if (found_trains_count > 0) {
                        // Ask the user to choose a train for seat booking
                        // Input: Train name or number
                        char user_input[MAX_TRAIN_LENGTH];
                        printf("Enter the train name or number: ");
                        scanf("%s", user_input);

                        // Check if the input matches a train number or name
                        int selected_train_index = -1;
                        for (int i = 0; i < train_count; i++) {
                            if (stricmp(trains[i].number, user_input) == 0 || stricmp(trains[i].name, user_input) == 0) {
                                selected_train_index = i;
                                break;
                            }
                        }

                        if (selected_train_index != -1) {
                            // Train found, ask the user to choose a coach
                            printf("Train Found - Train Number: %s, Train Name: %s\n", trains[selected_train_index].number,
                                   trains[selected_train_index].name);
                            // Input: Passenger details including date of travel
                            struct Passenger passenger;
                            printf("Enter your name: ");
                            scanf("%s", passenger.name);
                            printf("Enter your age: ");
                            scanf("%d", &passenger.age);
                            printf("Enter the date of travel (YYYY-MM-DD): ");
                            scanf("%s", passenger.date);

                            // Parse the date
                            struct tm travelDate;
                            if (!parseDate(passenger.date, &travelDate)) {
                                printf("Invalid date format. Please use YYYY-MM-DD.\n");
                                return 1;
                            }
                            // Check if the date is valid
                            if (isDateValid(&travelDate)) {

                                printf("Choose a coach:\n");
                                printf("1. Unreserved Coach (%d seats)\n", trains[selected_train_index].seats_unreserved);
                                printf("2. General Coach (%d seats)\n", trains[selected_train_index].seats_general);
                                printf("3. AC Coach (%d seats)\n", trains[selected_train_index].seats_ac);
                                printf("4. Sleeper Coach (%d seats)\n", trains[selected_train_index].seats_sleeper);

                                printf("Enter your choice (1-4): ");
                                scanf("%d", &cchoice);

                                // Update the number of available seats based on the user's choice and seat limits
                                switch (cchoice) {
                                    case 1:
                                        if (trains[selected_train_index].seats_unreserved > 0) {
                                            printf("Seat booked in Unreserved Coach.\n");
                                            //trains[selected_train_index].seats_unreserved--;
                                            //displayTicket(passenger, trains[selected_train_index],tickets[ticket_count - 1]);
                                        } else {
                                            printf("No available seats in Unreserved Coach.\n");
                                            break;
                                        }
                                        break;
                                    case 2:
                                        if (trains[selected_train_index].seats_general > 0) {
                                            printf("Seat booked in General Coach.\n");
                                            //trains[selected_train_index].seats_general--;
                                            //displayTicket(passenger, trains[selected_train_index],tickets[ticket_count - 1]);
                                        } else {
                                            printf("No available seats in General Coach.\n");
                                            break;
                                        }
                                        break;
                                    case 3:
                                        if (trains[selected_train_index].seats_ac > 0) {
                                            printf("Seat booked in AC Coach.\n");
                                            //trains[selected_train_index].seats_ac--;
                                            //displayTicket(passenger, trains[selected_train_index],tickets[ticket_count - 1]);
                                        } else {
                                            printf("No available seats in AC Coach.\n");
                                            break;
                                        }
                                        break;
                                    case 4:
                                        printf("%d", trains[selected_train_index].seats_sleeper);
                                        if (trains[selected_train_index].seats_sleeper > 0) {
                                            printf("Seat booked in Sleeper Coach.\n");
                                            //trains[selected_train_index].seats_sleeper--;
                                            //displayTicket(passenger, trains[selected_train_index],tickets[ticket_count - 1]);
                                        } else {
                                            printf("No available seats in Sleeper Coach.\n");
                                            break;
                                        }
                                        break;
                                    default:
                                        printf("Invalid choice.\n");
                                }
                            } else {
                                printf("Invalid travel date. Please choose a date within one month from today.\n");
                                break;
                            }


                            // Declare a ticket structure to be passed to the storeTicket function
                            struct Ticket ticket;
                            // Store the ticket if the date is valid
                            storeTicket(&ticket, passenger, trains[selected_train_index], &travelDate, source, destination,selected_train_index);




                            // Ask if the user wants to book another ticket
                            printf("Do you want to book another ticket? (1 for Yes, 0 for No): ");
                            scanf("%d", &bookAnotherTicket);
                        } else {
                            printf("Train not found.\n");
                        }
                    }
                }
                break;
            }

            case 2: {
                // Cancel a ticket
                int selected_train_index = -1;
                int serialNumber;
                char passengerName[MAX_STATION_LENGTH];
                printf("Enter the serial number of the ticket you want to cancel: ");
                scanf("%d", &serialNumber);
                printf("Enter the name on the ticket: ");
                scanf("%s", passengerName);
                // Call the updated function to remove the ticket from the CSV file
                removeTicketFromCSV("C:\\Users\\venka\\CLionProjects\\untitled\\cmake-build-debug\\booked_tickets.csv", serialNumber, trains[selected_train_index], passengerName, choice);

                // After removing the ticket, check if a ticket from the waiting list can be moved to booked tickets
                moveFromWaitingListToBookedTickets(trains[selected_train_index]);
                break;
            }

            default:
                printf("Invalid choice.\n");
                return 1;
        }
        printf("Do you want to book or cancel another ticket? (1 for Yes, 0 for No): ");
        scanf("%d", &continueBooking);
    }

    // Update the next serial number before exiting
    writeNextSerialNumber(nextSerialNumber);

    return 0;
}