# ejudge-new-standings
HTTP-server for generating standings tables using ejudge external logs

# Config files format

All config files should be placed in one directory. You can choose any directory you want, but directory like `/etc/ejudge-new-standings` recommended.

There is some basic server configuration in this file. You can place it in `/etc/ejudge-new-standings/config.xml`.

You can start server with: `java -jar ejudge-new-standings.jar /etc/ejudge-new-standings/config.xml`

Example:

## config.xml
```xml
<?xml version="1.0" encoding="utf-8" ?>
<server>
	<host>yourdomain.com</host>
	<port>8080</port>
	<contests>/home/judges</contests>
</server>
```

yourdomain.com is your host, 8080 is port that server will be using and `/home/judges` is `JUDGES_DIR`

By default server will start at given port. You can redirect it to 80 port using Apache virtual hosts for example.

For each standings table there should be config file. For example, if config file hase name `test.xml`, it can be reached at `http://yourdomain.com:8080/test`

Example:

## test.xml
```xml
<?xml version="1.0" encoding="utf-8" ?>
<config freeze="false" first_ac="true" empty_users="false" show_zero="true">
	<name>Test standings</name>
	<type>ICPC</type>
	<contests>
		<contest id="73" name="№10"/>
		<contest id="72" name="№9"/>
		<contest id="71" name="№8"/>
		<contest id="64" name="№7"/>
		<contest id="40" name="№6"/>
		<contest id="36" name="№5"/>
		<contest id="35" name="№4"/>
		<contest id="29" name="№3"/>
		<contest id="28" name="№2"/>
		<contest id="27" name="№1"/>
	</contests>
</config>
```

* `freeze` (true/false) if standings should be freezed
* `first_ac` (true/false) if first AC runs should be marked
* `empty_users` (true/false) if users without runs should be in standings
* `show_zero` (true/false) if users with zero score should be in standings
* `name` standings name
* `type` (ICPC/IOI) standings type
* `contests` contests list

For each contest you should specify its id and name.
