package com.ssgames.com.btc;

public class BTDataPacket {

	private String header;
	private String opCode;
	private String body;

	public BTDataPacket() {
		header = "HEADER";
	}

	public BTDataPacket(byte[] buffer) {
		setDataParts(buffer);
	}

	private void setDataParts(byte[] buffer) {

		try {
			String s = new String(buffer, "UTF-8");
			String[] sComps = s.split("#");
			if (sComps.length == 3) {
				header = sComps[0];
				opCode = sComps[1];
				body = sComps[2];
			}
		} catch (Exception e) {

		}
	}
	
	public byte[] getBuffer() {
		
		String s = header + "#" + opCode + "#" + body;
		byte[] buffer = null;
		try {
			buffer = s.getBytes("UTF-8");
		} catch (Exception e) {
			
		}
		return buffer;
	}

	public String getHeader() {
		return header;
	}

	public void setHeader(String header) {
		this.header = header;
	}

	public String getOpCode() {
		return opCode;
	}

	public void setOpCode(String opCode) {
		this.opCode = opCode;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}
