package xyz.yooniks.aegis.utils;

import net.md_5.bungee.api.ChatColor;
import xyz.yooniks.aegis.vpn.StringReplacer;

public class MessageBuilder {

  private String message;

  public static MessageBuilder newBuilder() {
    return new MessageBuilder();
  }

  public MessageBuilder withMessage(String message) {
    this.message = message;
    return this;
  }

  public MessageBuilder withField(String field, String replacement) {
    this.message = StringReplacer.replace(this.message, field, replacement);
    return this;
  }

  public MessageBuilder withField(String field, Object replacement) {
    this.message = StringReplacer.replace(this.message, field, String.valueOf(replacement));
    return this;
  }

  public MessageBuilder stripped() {
    this.message = StringReplacer.replace(this.message, "%nl%", "\n");
    return this;
  }

  public MessageBuilder coloured() {
    this.message = ChatColor.translateAlternateColorCodes('&', this.message);
    return this;
  }

  public String build() {
    return this.message;
  }

}
