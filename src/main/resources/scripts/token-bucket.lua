
-- KEYS[1] = client bucket key
-- ARGV[1] = capacity
-- ARGV[2] = refill tokens per second

local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])

local now = redis.call("TIME")
local currentTime =
    tonumber(now[1]) + tonumber(now[2]) / 1000000

local bucket = redis.call("HMGET",
    KEYS[1], "tokens", "updated")

local tokens = tonumber(bucket[1]) or capacity
local lastUpdated = tonumber(bucket[2]) or currentTime

local elapsed = math.max(0, currentTime - lastUpdated)

tokens = math.min(
    capacity,
    tokens + elapsed * refillRate
)

local allowed = 0
local retryAfter = 0

if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
else
    retryAfter = math.ceil((1 - tokens) / refillRate)
end

redis.call("HSET", KEYS[1],
    "tokens", tokens,
    "updated", currentTime)

local ttl = math.ceil(capacity / refillRate) + 1
redis.call("EXPIRE", KEYS[1], ttl)

return {allowed, math.floor(tokens), retryAfter}
