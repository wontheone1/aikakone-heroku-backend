# Aikakone backend part

Puzzle game using finna api. This game consists of frontend and backend part.
This repo is the backend part. Frontend is being developed in [another repo](https://github.com/wontheone1/aikakone).

## Running

* Inside REPL

```clj
(start-server)
```

* In the terminal

```sh
$ lein run
```

## Deploying to Heroku

```sh
$ heroku create aikakone-backend
$ git push heroku master
$ heroku ps:scale web=1
$ heroku open
$ heroku logs --tail
```

or

[![Deploy to Heroku](https://www.herokucdn.com/deploy/button.png)](https://heroku.com/deploy)
