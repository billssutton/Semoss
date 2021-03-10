package prerna.pyserve;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldPrepender;

public class TCPChannelInitializer extends ChannelInitializer<SocketChannel> {

	NettyClient nc = null;
	String inputSoFar = "";
	String endChar = "<o>";
	
	public void setClient(NettyClient nc)
	{
		this.nc = nc;
	}
	
	@Override
	protected void initChannel(SocketChannel ch) throws Exception {
		// TODO Auto-generated method stub
        ChannelPipeline p = ch.pipeline();
        
        TCPClientHandler nch = new TCPClientHandler();
        nch.setClient(nc);
        
        NettyClientHandler nch2 = new NettyClientHandler();
        nch2.setClient(nc);
        //p.addLast(new LoggingHandler(LogLevel.INFO));
        p.addLast(new LengthFieldPrepender(4));
        p.addLast(nch2);
		
	}

}
