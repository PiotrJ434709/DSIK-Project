#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <libgen.h>

void SendFile(int gn)
{
    long fileSize, sent, allSent, read;
    struct stat fileinfo;
    FILE* file;
    unsigned char buffer[1024];
    char path[512];

    memset(path, 0, 512);
    if (recv(gn, path, 512, 0) <= 0) {
        fprintf(stderr, "Blad przy odczycie sciezki\n");
        exit(1);
    }

    printf("Klient chce plik: %s\n", path);

    fileSize = 0;
    if (stat(path, &fileinfo) < 0) {
        printf("Nie moge pobrac informacji o pliku\n");
        fileSize = htonl(fileSize);
        if (send(gn, &fileSize, sizeof(long), 0) != sizeof(long)) {
            fprintf(stderr, "Blad przy wysylaniu wielkosci pliku: 0\n");
            exit(1);
        }
        return;
    }

    if (fileinfo.st_size == 0) {
        printf("Rozmiar pliku: 0\n");
        fileSize = htonl(fileSize);
        if (send(gn, &fileSize, sizeof(long), 0) != sizeof(long)) {
            fprintf(stderr, "Blad przy wysylaniu wielkosci pliku: 0\n");
            exit(1);
        }
        return;
    }

    printf("Dlugosc pliku: %ld\n", fileinfo.st_size);

    fileSize = (long) fileinfo.st_size;

    if (send(gn, &fileSize, sizeof(long), 0) != sizeof(long)) {
        fprintf(stderr, "Blad przy wysylaniu wielkosci pliku\n");
        exit(1);
    }

    fileSize = fileinfo.st_size;
    allSent = 0;

    file = fopen(path, "rb");
    if (file == NULL) {
        fprintf(stderr, "Blad przy otwarciu pliku\n");
        exit(1);
    }

    while (allSent < fileSize) {
        read = fread(buffer, 1, 1024, file);
        sent = send(gn, buffer, read, 0);
        if (read != sent)
            break;
        allSent += sent;
    }
    printf("Wyslano %ld bajtow \n", allSent);

    if (allSent == fileSize)
        printf("*** Plik wyslany poprawnie ***\n");
    else {
        fprintf(stderr, "*** Blad przy wysylaniu pliku ***\n");
        exit(1);
    }

    fclose(file);
    return;
}

void DownloadFile(int gn)
{
    char* fname;
    unsigned char buffer[1024];
    char path[512];
    FILE* file;
    long fileSize, received, allReceived;

    memset(path, 0, 512);
    if (recv(gn, path, 512, 0) <= 0) {
        fprintf(stderr, "Blad przy odbieraniu sciezki\n");
        exit(1);
    }

    printf("Sciezka pliku: %s\n", path);
    printf("Dlugosc sciezki: %ld\n", strlen(path));

    if (recv(gn, &fileSize, sizeof(long), 0) != sizeof(long)) {
        fprintf(stderr, "Blad przy odbieraniu dlugosci\n");
        exit(1);
    }

    printf("Plik ma dlugosc: %li\n", fileSize);

    allReceived = 0;

    fname = basename(path);
    file = fopen(fname, "ab");
    if (NULL == file) {
        fprintf(stderr, "Blad przy otwieraniu pliku");
        exit(1);
    }

    printf("Pobieram plik o nazwie %s...\n", fname);

    while (allReceived < fileSize) {
        memset(buffer, 0, 1024);
        received = recv(gn, buffer, 1024, 0);
        if (received < 0)
            break;
        allReceived += received;
        fwrite(buffer, 1, received, file);
    }

    printf("Odebrano %ld bajtow \n", allReceived);
    if (allReceived != fileSize) {
        fprintf(stderr, "*** Blad w odbiorze pliku ***\n");
        exit(1);
    }
    else
        printf("*** Plik odebrany poprawnie ***\n");

    fclose(file);
    return;
}

void ServeConnection(int gn)
{
    unsigned char option;

    while (1) {
        printf("Czekam na polecenia klienta...\n");
        if (recv(gn, &option, 1, 0) <= 0) {
            fprintf(stderr, "Blad przy odczycie opcji\n");
            exit(1);
        }

        switch (option) {
        case '1':
            SendFile(gn);
            break;
        case '2':
            DownloadFile(gn);
            break;
        case '3':
            return;
        }
    }
}

int main(int argc, char** argv)
{
    int sock_listen, sock_client;
    struct sockaddr_in adr;
    socklen_t adrlen = sizeof(struct sockaddr_in);

    if (argc < 2) {
        printf("Podaj numer portu jako parametr\n");
        return 1;
    }

    sock_listen = socket(PF_INET, SOCK_STREAM, 0);

    adr.sin_family = AF_INET;
    adr.sin_port = htons(atoi(argv[1]));
    adr.sin_addr.s_addr = INADDR_ANY;

    memset(adr.sin_zero, 0, sizeof(adr.sin_zero));

    if (bind(sock_listen, (struct sockaddr*)&adr, adrlen) < 0) {
        printf("Bind nie powiodl sie\n");
        return 1;
    }

    listen(sock_listen, 10);

    while (1) {
        adrlen = sizeof(struct sockaddr_in);
        sock_client = accept(sock_listen, (struct sockaddr*)&adr, &adrlen);
        if (sock_client < 0) {
            printf("Accept zwrocil blad\n");
            continue;
        }

        printf("Polaczenie od %s:%u\n",
            inet_ntoa(adr.sin_addr),
            ntohs(adr.sin_port));

        if (fork() == 0) {
            /* proces potomny */
            printf("Zaczynam obsluge...\n");
            ServeConnection(sock_client);

            printf("Klient zakonczyl polaczenie\n");
            close(sock_client);
            exit(0);
        }
        else
            /* proces macierzysty */
            continue;
    }
    return 0;
}
