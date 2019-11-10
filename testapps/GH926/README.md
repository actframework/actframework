# GH926 CLI - it shall not use JSON as default encoding type for output

## Reproduce

1. run `run_dev`

2. Once app started, enter cli

3. type `hello world`, it will print out something like

   ```json
   {"result": "hello world"}
   ```

   