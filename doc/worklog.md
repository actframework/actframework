# Work Log

## Issue #15: Support @async annotation on public void method

* Created branch i15
* Updated `SimpleEventListenerMetaInfo`: Added `asyncMethodName` field
* Updated `SimpleEventListenerScanner`: for `@Async` annotated `public` `void` and non `abstract` method, set `asyncMethodName` to the meta info

TODO:

### Async method enhancer

* Clone the async method with async method name
* Update async method body by event dispatching

 