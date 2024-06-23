# IP Security Suite -- Your one-stop-shop for all things relating to IP filtration!
![A stickman blocks a hacker from joining the server using the IP Security Suite](https://cdn.modrinth.com/data/cached_images/29dce2fc6a38138d2711fb406298b72483290e64.png)

_Important Note: On servers where this mod is installed, there is an option to reveal players' IPs to operators. This works great for smaller, personal servers, but it can potentially lead to doxxing on larger ones. Stay safe!_

## The core of IP Security Suite -- `/ipss`

All IP Security Suite subcommands are implemented under the `/ipss` command.

## Set up VPN blocking

To set up VPN blocking, you will need to use the API provided for free by https://vpnapi.io and make an account.

After creating an account, you will be given an API key. Add it to your server by running `/ipss vpn api <YOUR API KEY>`. Also make sure to do `/ipss config set vpn false`.

## IP-Lock players

You might want to IP-lock an operator on a server to make sure that they can only log in from one IP address.

- `/ipss lock` locks your own account to an IP. An account can be locked to multiple IPs simultaneously.
- `/ipss lock other <other_player>` locks another player to an IP
- `/ipss lock erase <other_player>` erases all IP locks
- `/ipss lock last <other_player>` adds a lock for the last attempted IP login.

When you first lock your account, all future logins from other locations are blocked. If you want to access your account from another location, then **first, try connecting to the server**. The mod will remember the IP with which you connected. Then, from the server terminal, type `/ipss lock last <your_username>` -- this will add the new IP and allow you to log on.

## Configuration settings

### Strict mode

By default, the server will allow anyone without an IP-lock to log on freely from any location. Strict mode disables this behaviour, so if a player is not registered at all, they cannot log on, essentially acting like a whitelist.

### VPN Logons

By default, the server will allow anyone with a VPN or proxy to log on, even when an API key is set up. To block users with VPNs from logging on, then do `/ipss config set vpn false` and set up an API key as outlined above.

### Apply a lock to yourself without operator

By default, the server will you to lock only your own IP if you don't have operator, but by doing `/ipss config set self-lock false` disables this behavior, so only operators can set up IP locks for players.
