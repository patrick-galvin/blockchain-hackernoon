# blockchain-hackernoon

Clojure blockchain implementation based on the tutorial from [Hackernoon blockchain article](https://hackernoon.com/learn-blockchains-by-building-one-117428612f46)

Examples in how to use yada/bidi/integrant

## Getting Started

```
lein run
```

## Endpoints

| Operation               | Method        |   Body  |
| ----------------------- |:-------------:|---------|
| [Mine](http://localhost:3000/mine)                        | GET  | |
| [Transaction](http://localhost:3000/transaction/new)      | POST | `{"sender":"73676424-7bff-4e06-a96b-47c2ed619753",` |
|                                                           |      |  `"recipient":"73676424-7bff-4e06-a96b-47c2ed619753",`|
|                                                           |      | `"amount":1000}`|
| [Register new node](http://localhost:3000/nodes/register) | POST | `["http://192.168.0.1:5000"]` |
| [Reconcile nodes](http://localhost:3000/nodes/resolve)    | GET | 


## Built With

* [Yada](https://github.com/juxt/yada)
* [Bidi](https://github.com/juxt/bidi)
* [Integrant](https://github.com/weavejester/integrant)