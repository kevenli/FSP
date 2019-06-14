package flowy.scheduler.rest;

import com.sun.corba.se.impl.protocol.giopmsgheaders.MessageBase;
import flowy.scheduler.server.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsumerController {
    @Autowired
    private Session session;

    @GetMapping("/send")
    public String send() {
//        MessageBase.Message message = new MessageBase.Message()
//                .toBuilder().setCmd(MessageBase.Message.CommandType.NORMAL)
//                .setContent("hello server")
//                .setRequestId(UUID.randomUUID().toString()).build();
        session.onNotify(1);
        return "send ok";
    }
}