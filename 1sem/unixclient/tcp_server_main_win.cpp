/* SERVER */
#include "packet.h"
#include <sys/types.h>
#include <sys/socket.h>
#include <sys/stat.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <assert.h>
#include <iostream>
#include <pthread.h>
#include <dirent.h>


#pragma pack(push, 1)
struct Mail
{
	unsigned char  addrLen;
	unsigned char  themeLen;
	unsigned short msgLen;
};
#pragma pack(pop)


#define SERV_ADDR "192.168.0.122"
#define SERV_PORT 3000
#define MAX_CLIENTS 5

struct Client
{
	int sock;
	Packet pack;
	std::string login;
	bool   connected;
};

// command line handler
void* cmdHandler(void* arg);
// accept connections to listen socket
void AcceptConnections(int ListeningSocket);
// new thread to connected client
void* ClientHandler(void* arg);

// get list of mail in directory
unsigned int listOfMail(const std::string & dir, char * buf, const size_t buf_size);
// get file name at selected index
bool fileAtIndex(const std::string & dir, int index, std::string & filename);

// check if a file exists
bool fileExists(const std::string & path);
// check if a directory exists
bool dirExists(const std::string & path);
// create directory
bool createDir(const std::string & path);

// global vars
Client clients[MAX_CLIENTS];
bool run;
int ListenSocket;

int main()
{
	ListenSocket = socket(AF_INET,SOCK_STREAM,0);
	if (ListenSocket == SOCKET_ERROR)
	{
		printf("Can't create socket\n");
		return 1;
	}

	struct sockaddr_in server_addr;

	memset(&server_addr, 0, sizeof(server_addr));
    server_addr.sin_family = AF_INET;
	server_addr.sin_addr.s_addr = inet_addr(SERV_ADDR);
	server_addr.sin_port = htons(SERV_PORT);
    
	if (bind(ListenSocket, (struct sockaddr *)&server_addr, sizeof(struct sockaddr)) == SOCKET_ERROR)
	{
		printf("bind failed with error %s\n", strerror(errno));
		close(ListenSocket);
		return 1;
	}
	else
		printf("Starting server at %s:%d\n",SERV_ADDR,SERV_PORT);

	if (listen(ListenSocket, MAX_CLIENTS) == SOCKET_ERROR)
		printf("listen function failed with error: %s\n", strerror(errno));

	printf("Waiting for clients to connect...\n");

	for (int i=0; i<MAX_CLIENTS; i++)
		clients[i].connected = false;

	if (!dirExists("MAIL"))
		createDir("MAIL");
    
    run = true;
    
    // start new thread for commands
    pthread_t threadID;
    pthread_create( &threadID, NULL, cmdHandler, 0);

	AcceptConnections(ListenSocket);
	
    


	return 0;
}

void* cmdHandler(void* arg)
{
    while (run)
    {
        std::string cmd;
        getline(std::cin, cmd);
        if (cmd.empty())
            continue;
        
        if (cmd == "exit" || cmd == "e")
        {
            run = false;
        }
        else if (cmd == "kick" || cmd == "k")
        {
            std::string login;
            std::cout << "Type login:";
            getline(std::cin, login);
            // kick
            int i;
            for (i=0; i<MAX_CLIENTS; i++)
            {
                if (clients[i].connected && clients[i].login == login)
                {
                    clients[i].connected = false;
                    close( clients[i].sock );
                    clients[i].sock = 0;
                    break;
                }
            }
            if (i == MAX_CLIENTS)
            {
                std::cout << "Can't kick \"" << login << "\" because that user not connected" << std::endl;
            }
        }
        else if (cmd == "users" || cmd == "u")
        {
            for (int i=0; i<MAX_CLIENTS; i++)
            {
                if (clients[i].connected)
                    std::cout << clients[i].login << std::endl;
            }
        }
    }
    
    close(ListenSocket);
    ListenSocket = 0;
    
    return 0;
}

void AcceptConnections(int ListeningSocket)
{
	sockaddr_in client_addr;
	int nAddrSize = sizeof(sockaddr_in);

	while (run)
	{
        int sd = accept(ListeningSocket, (struct sockaddr *)&client_addr, (socklen_t *)&nAddrSize);
		if (sd != SOCKET_ERROR)
		{
            printf("I got a connection from %s:%d\n",
                   inet_ntoa(client_addr.sin_addr),ntohs(client_addr.sin_port));
			bool accepted = false;
			for (int i=0; i<MAX_CLIENTS; i++)
			{
				if (!clients[i].connected)
				{
					clients[i].connected = true;
					clients[i].sock = sd;
					printf("Accepted connection from %s:%d\n",inet_ntoa(client_addr.sin_addr),ntohs(client_addr.sin_port));

					pthread_t nThreadID;
                    pthread_create( &nThreadID, NULL, ClientHandler, (void*) &clients[i]);
					
					accepted = true;
					break;
				}
			}

			if (accepted)
				continue;

			printf("Can't accept more then %d clients\n",MAX_CLIENTS);
			close(sd);
		}
		else 
		{
            // Software caused connection abort
            if (errno != ECONNABORTED)
                printf("accept() failed (%s)\n",strerror(errno));
            
			break;
		}
	}
    
    for (int i=0; i<MAX_CLIENTS; i++)
    {
        if (clients[i].connected)
        {
            clients[i].connected = false;
            close( clients[i].sock );
            clients[i].sock = 0;
        }
    }

}

void* ClientHandler(void* arg)
{
	Client * client = (Client*)arg;
	printf("Starting new Client\n");
	
	printf("Waiting for commands\n");

	sleep(5);
	while (run && client->connected)
	{
		int res;

		res = waitForPacket(client->sock, &client->pack);
		// check for errors
		if (res == 0)
		{
			// client was disconnected
			break;
		}
		else if (res < 0)
		{
            // bad socket descriptor
            if (errno == EBADF && client->connected==false)
            {
                // break client cycle because client was kicked by server
                break;
            }
            else
            {
                // error
                std::cout << "Error while receive packet (" << strerror(errno) << ") from " << client->login << std::endl;
                break;
            }
		}

		// process packet
		if (client->pack.code == C_GET_MAIL_LIST)
		{
			std::string path = "MAIL/";
			path += client->login;
			if (!dirExists(path))
			{
				// no mail for current user
				sendResponse(client->sock, &client->pack, "Ior boks is empti, lozer");
			}
			else
			{
				resetPacket(&client->pack);
				client->pack.code = C_RESPONSE;
				client->pack.dataSize = listOfMail(path,client->pack.data,BUF_MAX_SIZE);
				sendPacket(client->sock, &client->pack);
			}
		}
		else if (client->pack.code == C_SEND_MAIL)
		{
			Mail * mail = (Mail*)client->pack.data;
			std::string filename = "MAIL/";
			filename += client->pack.data+sizeof(Mail);

			// create user dir
			if (!dirExists(filename))
				createDir(filename);

			filename += '/';
			filename += client->login;
			filename += '-';
			filename += client->pack.data+mail->addrLen+1+sizeof(Mail);

			if (fileExists(filename))
			{
				// send error, because file already exists
				sendResponse(client->sock, &client->pack, "U hav olredi sent a imeil wit such them");
			}
			else
			{
				FILE * pFile = fopen(filename.c_str(), "w");

				if (pFile==NULL)
				{
					sendResponse(client->sock, &client->pack, "Unaibl tu oupen fail to rait");
				}
				else
				{
					fprintf(pFile, "Author: %s\n",client->login.c_str());
					fprintf(pFile, "Theme: %s\n", client->pack.data+mail->addrLen+1+sizeof(Mail));
					fprintf(pFile, "%s", client->pack.data+mail->addrLen+mail->themeLen+2+sizeof(Mail));

					fclose(pFile);

					sendResponse(client->sock, &client->pack, "Ior mesedg send");
				}

			}
		}
		else if (client->pack.code == C_RECV_MAIL)
		{
			int index = 0;
			index |= client->pack.data[0];
			index |= client->pack.data[1]<<8;
			std::string filename;
			std::string path = "MAIL/";
			path += client->login;

			if (fileAtIndex(path,index,filename))
			{
				path += '/';
				path += filename;
				// found mail
				FILE * pFile;
				long lSize;
				
				pFile = fopen ( path.c_str() , "rb" );
				if (pFile==NULL) 
				{
					// can't open mail
					sendResponse(client->sock, &client->pack, "Ne udaetsja otkryt' fail");
				}
				else
				{
					fseek (pFile , 0 , SEEK_END);
					lSize = ftell (pFile);
					rewind (pFile);

					assert(lSize+1 < BUF_MAX_SIZE);

					resetPacket(&client->pack);
					client->pack.code = C_RESPONSE;
					client->pack.dataSize = (int)lSize+1;
					fread(client->pack.data,1,lSize,pFile);
					client->pack.data[lSize] = '\0';

					sendPacket(client->sock, &client->pack);

					fclose(pFile);
				}
			}
			else
			{
				// mail not found
				sendResponse(client->sock, &client->pack, "Trai too fks ur hends and chuz the korekt aidi");
			}
		}
		else if (client->pack.code == C_LOGIN)
		{
			printf("%s logged in\n",client->pack.data);
			client->login = client->pack.data;
			sendResponse(client->sock, &client->pack, "Yu ar logined, congaretulaisenz!");
		}
	}
	printf("Closing client(%s) socket\n",client->login.c_str());
	
    if (client->sock)
        close(client->sock);

	return 0;
}

unsigned int listOfMail(const std::string & dir, char * buf, const size_t buf_size)
{
	DIR *pDir;
	struct dirent *ent;
	unsigned int count = 0;
	int id = 1;

	/* Open directory stream */
	pDir = opendir (dir.c_str());
	assert (pDir != NULL);
	
	/* Print all files and directories within the directory */
	while ((ent = readdir (pDir)) != NULL) 
	{
		if (ent->d_name[0] == '.')
			continue;

		if (ent->d_type == DT_REG) 
		{
			char polina[256]; 
			sprintf(polina, "%d %s\n", id, ent->d_name);
			assert (count+strlen(polina)+1 < buf_size);
			strcpy(buf+count, polina);
			//printf ("%s (%d)\n", ent->d_name,ent->d_namlen);
			count += strlen(polina);
			id++;
		}
	}

	if (count != 0)
	{
		buf[count-1] = '\0';
	}

	closedir (pDir);

	return count;
}

bool fileAtIndex(const std::string & dir, int index, std::string & filename)
{
	DIR *pDir;
	struct dirent *ent;
	int count = 0;

	/* Open directory stream */
	pDir = opendir (dir.c_str());
	if (pDir == NULL)
        return false;

	/* Print all files and directories within the directory */
	while ((ent = readdir (pDir)) != NULL) 
	{
		if (ent->d_name[0] == '.')
			continue;

		if (ent->d_type == DT_REG) 
		{
			count++;
			if (count == index)
			{
				filename = ent->d_name;
				return true;
			}
		}
	}

	closedir(pDir);

	return false;
}

bool fileExists(const std::string & path)
{
	struct stat sb;

	return (stat (path.c_str(), &sb) == 0 && S_ISREG(sb.st_mode));
}

bool dirExists(const std::string & path)
{
	struct stat sb;
    
	return (stat (path.c_str(), &sb) == 0 && S_ISDIR(sb.st_mode));
}

bool createDir(const std::string & path)
{
	return (mkdir(path.c_str(), 0777) != -1);
}
