# telegram_bot_be
This is a backend program about OpenAI's involvement in telegram bot

Please modify the configuration file in `resources/application.properties` first

```properties
# replace the token with your own token
openai.key=you token
# telegram
bot.token=you bot token
bot.username=you bot username

# this attribute is how many sessions can be accepted by your backend configuration
max.session.len=20

# If your network condition is restricted, you can start the proxy, but you need to have a proxy address
proxy.open=false
proxy.host=127.0.0.1
proxy.port=7890
```

## Build
`mvn clean package`