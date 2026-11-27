-- OpenOS runs this when the floppy is mounted, with the floppy's filesystem as its argument: run workerd from it.
local proxy = ...
local filesystem = require("filesystem")
local shell = require("shell")
for mounted, path in filesystem.mounts() do
  if mounted == proxy then
    shell.execute(filesystem.concat(path, "workerd.lua"))
    return
  end
end
