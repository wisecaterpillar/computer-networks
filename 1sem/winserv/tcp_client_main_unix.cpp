#include <WinSock2.h>
#include <assert.h>
#include <string>
#include <iostream>
#include "packet.h"


using namespace std;
#pragma comment(lib, "Ws2_32.lib")

#pragma pack(push, 1)
struct Mail
{
	unsigned char  addrLen;
	unsigned char  themeLen;
	unsigned short msgLen;
};
#pragma pack(pop)

// connect to server
void connect(const char * addr, const char * port);
// disconnect from server (if connected)
void disconnect();
// Check contents of your mail-list
void checkMail();
// Send mail to another user 
void sendMail();
// Receive mail from server
void recvMail();

bool isConnected;
bool isFinished;
SOCKET ClientSocket;
Packet pack;

int main()
{
	
	WSADATA wsaData;

	if (WSAStartup(MAKEWORD(2, 2), &wsaData) != NO_ERROR) 
	{
		cout << "Error at WSAStartup()" << endl;
		return 1;
	}

	isConnected = false;
	isFinished = false;

	/*
	User commands:
	(c)onnect *addr and port* - connect to server
	(d)isconnect - Disconnect from server
	(ch)eck - Check contents of your mail-list
	(s)end - Send Message to someone
	(r)eceive <Mail ID> - Recieve exact letter
	(h)elp - Help
	(e)xit - exit program
	*/

	cout<<"--------Client--------"<<endl<<"Use help OR h for help"<<endl;

	while (!isFinished)
	{
		cout << ">";

		string cmd;
		getline(cin, cmd);

		if (cmd.empty()) 
			continue;

		if (cmd == "connect" || cmd == "c")
		{
			// connect to addr
			cout << "Enter server IP address:";
			string ip,port;
			getline(cin, ip);
			cout << "Enter server port:";
			getline(cin, port);

			connect(ip.c_str(),port.c_str());
		}
		else if (cmd == "disconnect" || cmd == "d")
		{
			// disconnect
			disconnect();
		}
		else if (cmd == "check" || cmd == "ch")
		{
			// check mail
			checkMail();

		}
		else if (cmd == "send" || cmd == "s")
		{
			// send mail
			sendMail();	
		}
		else if (cmd == "receive" || cmd == "r")
		{
			// receive mail
			recvMail();
		}
		else if (cmd == "help" || cmd == "h")
		{
			// help
			cout << "User commands:" << endl;
			cout << "(c)onnect *addr and port* - connect to server" << endl;
			cout << "(d)isconnect - Disconnect from server" << endl;
			cout << "(ch)eck - Check contents of your mail-list" << endl;
			cout << "(s)end - Send Message to someone" << endl;
			cout << "(r)eceive <Mail ID> - Recieve exact letter" << endl;
			cout << "(h)elp - Help" << endl;
			cout << "(e)xit - exit program" << endl;
		}
		else if (cmd == "exit" || cmd == "e")
		{
			// exit
			if (isConnected)
				disconnect();
			isFinished = true;
		}
		else
		{
			cout << "Bad command, type \"help\" or \"h\" for help" << endl;
		}

		
	}


	closesocket(ClientSocket);
	WSACleanup();

	return 0;
}

// connect to server
void connect(const char * addr, const char * port)
{
	if (isConnected)
		disconnect();

	cout << "Connecting to "<< addr << ":" << port << endl;

	sockaddr_in sockAddr;

	sockAddr.sin_family = AF_INET;
	sockAddr.sin_addr.s_addr = inet_addr(addr);

	if (sockAddr.sin_addr.s_addr == INADDR_NONE ||
		sockAddr.sin_addr.s_addr == INADDR_ANY)
	{
		cout << "Can't convert \"" << addr <<"\" to valid IP address" << endl;
		return;
	}
	sockAddr.sin_port = htons(atoi(port));

	// create socket
	ClientSocket = socket(AF_INET,SOCK_STREAM,0);
	if (ClientSocket == INVALID_SOCKET)
	{
		cout << "Can't create socket" << endl;
		return;
	}


	if (connect(ClientSocket,(sockaddr*)&sockAddr, sizeof(sockAddr)) == SOCKET_ERROR)
	{
		cout << "Can't connect to "<<addr<<":"<<atoi(port)<<" because "<< WSAGetLastError()<<endl;
		return;
	}
	else
	{
		isConnected = true;
		cout << "Connected to "<<addr<<":"<<atoi(port)<<endl;

		cout << "Enter your login: ";
		string login;
		getline(cin, login);
		if ( login.length() < 2 )
		{
			cout << "Invalid login" << endl;
			disconnect();
			return;
		}
		resetPacket(&pack);
		pack.code = C_LOGIN;
		pack.dataSize = login.length()+1;
		strcpy(pack.data,login.c_str());
		printf("Logging in...\n");
		if (!sendPacket(ClientSocket,&pack))
		{
			cout << "Can't send login to server" << endl;
			disconnect();
		}

		if (waitForPacket(ClientSocket, &pack) != 1)
		{
			disconnect();
			return;
		}


		if (pack.code != C_RESPONSE)
		{
			cout << "Bad packet from server, command must be C_RESPONSE";
		}
		else
		{
			// print packet
			cout << pack.data;

		}

		cout << endl;
	}
}

// disconnect from server (if connected)
void disconnect()
{
	if (!isConnected)
	{
		cout << "No connection" << endl;
		return;
	}

	cout << "Breaking connection" << endl; 

	shutdown(ClientSocket,SD_BOTH);
	closesocket(ClientSocket);
	isConnected = false;
}

// Check contents of your mail-list
void checkMail()
{
	if (!isConnected)
	{
		cout << "No connection" << endl;
		return;
	}

	// send request
	if (!sendPacket(ClientSocket, C_GET_MAIL_LIST))
		return;
	

	// wait packet with response 5 sec
	if (waitForPacket(ClientSocket, &pack) != 1)
	{
		disconnect();
		return;
	}

	if (pack.code != C_RESPONSE)
	{
		cout << "Bad packet from server, command must be C_RESPONSE";
	}
	else
	{
		// print packet
		cout << pack.data;
	}

	cout << endl;
}

// Send mail to another user 
void sendMail()
{
	/*
	client asks user about addressee, theme 
	and contents of the mail 
	and consequentially sends it to server
	*/

	if (!isConnected)
	{
		cout << "No connection" << endl;
		return;
	}

	string sAddr,sTheme,sMsg;
	cout << "Enter addressee username: ";
	getline(cin, sAddr);
	cout << "Enter theme: ";
	getline(cin, sTheme);
	cout << "Enter message: ";
	getline(cin, sMsg);

	assert (sAddr.length()+sTheme.length()+sMsg.length()+sizeof(Mail)+3<BUF_MAX_SIZE);

	resetPacket(&pack);
	pack.code = C_SEND_MAIL;
	pack.dataSize = sAddr.length()+sTheme.length()+sMsg.length()+sizeof(Mail)+3;

	Mail * mail = (Mail*)pack.data;

	mail->addrLen  = sAddr.length();
	mail->themeLen = sTheme.length();
	mail->msgLen   = sMsg.length();

	strcpy(pack.data+sizeof(Mail),sAddr.c_str());
	strcpy(pack.data+sizeof(Mail)+sAddr.length()+1,sTheme.c_str());
	strcpy(pack.data+sizeof(Mail)+sAddr.length()+sTheme.length()+2,sMsg.c_str());

	// send request
	if (!sendPacket(ClientSocket, &pack))
		return;

	if (waitForPacket(ClientSocket, &pack) != 1)
	{
		disconnect();
		return;
	}


	if (pack.code != C_RESPONSE)
	{
		cout << "Bad packet from server, command must be C_RESPONSE";
	}
	else
	{
		// print packet
		cout << pack.data;

	}

	cout << endl;
}

// Receive mail from server
void recvMail()
{
	if (!isConnected)
	{
		cout << "No connection" << endl;
		return;
	}

	string sMailID;
	cout << "Enter mail ID to receive: ";
	getline(cin, sMailID);

	int id = atoi(sMailID.c_str());

	if (id==0)
	{
		cout << "Can't convert " << sMailID << " to number, ID must be greater then 0" << endl;
		return;
	}

	resetPacket(&pack);
	pack.code = C_RECV_MAIL;
	pack.dataSize = 2;
	pack.data[0] = id & 0xFF;
	pack.data[1] = (id>>8) & 0xFF;

	// send request
	if (!sendPacket(ClientSocket, &pack))
		return;
	

	// wait packet with response 5 sec
	if (waitForPacket(ClientSocket, &pack) != 1)
	{
		disconnect();
		return;
	}
	
	// process packet
	if (pack.code != C_RESPONSE)
	{
		cout << "Bad packet from server, command must be C_RESPONSE";
	}
	else
	{
		// print packet
		cout << pack.data;

	}

	cout << endl;
	
}
