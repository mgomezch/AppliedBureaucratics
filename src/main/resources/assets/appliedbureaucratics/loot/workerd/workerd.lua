-- workerd: clocks this robot in at its Docking Pad, forever.
-- "Half of all traffic is bots talking to bots."
local component = require("component")

if not component.isAvailable("requisition") then
  io.stderr:write("workerd: no Requisition Upgrade installed. Please contact HR.\n")
  return 1
end

local requisition = component.requisition
print("workerd: clocked in. Thank you for your service.")
while true do
  requisition.work()
  os.sleep(1)
end
