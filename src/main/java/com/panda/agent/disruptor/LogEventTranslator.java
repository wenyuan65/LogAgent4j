package com.panda.agent.disruptor;

import com.lmax.disruptor.EventTranslatorOneArg;

public class LogEventTranslator implements EventTranslatorOneArg<LogEvent, String> {

    /** 文件所属的游戏代号 */
    private String game;
    /** 服务器名称 */
    private String serverName;

    public LogEventTranslator(String game, String serverName) {
        this.game = game;
        this.serverName = serverName;
    }

    @Override
    public void translateTo(LogEvent logEvent, long l, String s) {
        logEvent.setRawContent(s);

        logEvent.put("game", game);
        logEvent.put("server", serverName);
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }
}
