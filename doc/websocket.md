# WebSocket support

**Note** WebSocket support is done there is big differences between the implementation of the proposed API as described below

## Findings

* new WebSocket(...) client API does not support specifying http headers
* new WebSocket(...) client API support specifying subprotocol as second parameter. Normally subprotocol could be "soap", "json" etc 

## Security consideration

It looks like websocket request will attach the cookies. Which could be used to pass security credentials. However if project is an android/ios device does not allow cookie, then we need to design other way to pass security credentials. Below is one proposed solution:

1. do not authenticate/authorize on `Connection` event

1. client send credential (in any form understandable by backend app) immediately after connection is established. This needs to be a standard key/value. Suggested form:

	```json
    {
    	"act_credential": "..."
    }
    ```

1. upon `act_credential` message, server will store the credential along with the WS session


## Proposed usage scenario

### Chat room

Backend code

```java
public class ChatRoom {

	@WsOpen("/chatroom/{roomId}") 
    public void connect(int roomId, WsSession session) {
    	ChatRoom chatRoom = chatRoomDao.findById(roomId);
        notFoundIfNull(chatRoom);
        chatRoom.add(session);
    }
    
    @WsClose("/chatroom/{roomId}")
    public void disconnect(int roomId, WsSession session) {
    	ChatRoom chatRoom = chatRoomDao.findById(roomId);
        notFoundIfNull(chatRoom);
        chatRoom.remove(session);
    }
    
    @WsMessage("/chatroom/{roomId}/send")
    public void sendMessage(int roomId, String message) {
    	ChatRoom chatRoom = chatRoomDao.findById(roomId);
        notFoundIfNull(chatRoom);
        chatRoom.send(message);
    }

}
```

