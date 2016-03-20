#ifndef __PACKET_H__
#define __PACKET_H__

#include <WinSock2.h>

#define MAGIX_NUMBER 0xDEAD
#define BUF_MAX_SIZE 1000

#pragma pack(push, 1)
struct Packet
{
	int		magix;
	int		code;
	unsigned int	dataSize;
	char 	data[BUF_MAX_SIZE];
};
#pragma pack(pop)

#define HEADER_SIZE (sizeof(Packet)-BUF_MAX_SIZE)

enum CODES {C_GET_MAIL_LIST = 1, C_SEND_MAIL, C_RECV_MAIL, C_RESPONSE, C_LOGIN};


// reset fields to default values
void resetPacket(Packet * pack);
// fill packet
void fillPacket(Packet * pack, void * data, size_t len);
// send packet to connected server
bool sendPacket(SOCKET sock, int code);
// send packet to connected server
bool sendPacket(SOCKET sock, Packet * pack);
// send raw data
bool sendRaw(SOCKET sock, char * data, int len);
// wait for packet. Return values: if less then 0, error occurred, else if 0, connection with client was close, else if 1 - success 
int waitForPacket(SOCKET sock, Packet * pack);

#endif // __PACKET_H__
