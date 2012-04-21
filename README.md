
particl
=======

Taking email back.

What is this?
-------------

At the moment, very little.

I am looking for a new project to work on - one that could be important in
ways that matter to me.  Looking around, I see quite a bit of discussion about
how email should be improved, more discussion about how bad it is for Google
to control so much data, and various more general concerns about privacy.

So this is an attempt to make a cross-platform email client that gives people
control over their email again.  That's the initial medium-term goal.  The
longer term goal is to extend this to other social media.  All while adding
decent crypto built around unsafe (but practical - the ssh compromise when
connecting to an unknown site) public key crypto.

Tell me more!
-------------

OK, here are various random notes:

 - It's web based.  There's no need, for a local mail client, but it's what I
   know, and it keeps options open for future use (eg when IPV6 means
   everything is online).

 - It uses Clojure and clojurescript.  The client will be "smart".  Because
   that's the language I'm happiest with right now, and it lets me leverage
   Java libraries for a lot of the hard work.

 - It will grab email from Google via IMAP and save it to local disk,
   optionally deleting.

 - When email is sent, a public key is included in headers.  Public keys
   received are kept in a database.  Email sent to someone with a known public
   key will be automatically encrypted.

 - It will use a local database (think mairix) to provide fast search.

 - The client will be simple, but will have a plugin architecture.  And I
   guess the client too.  Am reading open source architecture book right now.
   Need to think about lessons from that.

 - Need to choose a licence.  I prefer GPL but perhaps MIT will help growth?

That's all for now,

Andrew
