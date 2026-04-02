box.cfg{
    listen = 3301,
    wal_mode = "write",
    log_level = 5
}

box.schema.upgrade()

if box.space.KV then
    box.space.KV:drop()
    print("Old KV space dropped")
end

local kv = box.schema.space.create('KV', { engine = 'memtx' })
kv:format({
    { name = 'key', type = 'string' },
    { name = 'value', type = 'varbinary', is_nullable = true },
})
kv:create_index('primary', { type = 'TREE', parts = { 'key' } })
print("KV space created")

box.schema.user.create('user', { password = 'password', if_not_exists = true })
box.schema.user.grant('user', 'super', nil, nil, { if_not_exists = true })

crud = {}

function crud.replace(space_name, tuple)
    local key = tuple[1]
    local value = tuple[2] ~= nil and tuple[2] or box.NULL
    local res = box.space[space_name]:replace{key, value}

    return res
end

function crud.get(space_name, key)
    local t = box.space[space_name]:get(key)

    return t and {t} or {}
end

function crud.delete(space_name, key)
    local res = box.space[space_name]:delete(key)

    return res
end

function crud.count(space_name)
    local c = box.space[space_name]:count()

    return {c}
end

function crud.select(space_name, conditions, opts)
    local space = box.space[space_name]
    local result = {}

    for _, tuple in space:pairs() do
        local key = tuple[1]
        local ok = true

        for _, cond in ipairs(conditions or {}) do
            local field, op, value = cond[1], cond[2], cond[3]
            if field == 'key' then
                if op == '>=' and not (key >= value) then ok = false end
                if op == '>'  and not (key > value)  then ok = false end
                if op == '<'  and not (key < value)  then ok = false end
            end
        end

        if ok then
            table.insert(result, tuple)
        end
    end

    table.sort(result, function(a,b) return a[1] < b[1] end)

    local limit = opts and opts.first or #result
    local rows = {}

    for i = 1, math.min(limit, #result) do
        table.insert(rows, result[i])
    end

    return {
        {
            rows = rows
        }
    }
end
