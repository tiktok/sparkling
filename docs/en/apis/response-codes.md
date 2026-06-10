# Response codes

Sparkling Method response codes follow the Android/iOS native bridge convention.
This convention is the source of truth for `sparkling-method`, generated method
wrappers, and built-in method packages.

| Code | Meaning |
| --- | --- |
| `1` | Success |
| `0` | Method executed but failed |
| Negative | Bridge, parameter, permission, timeout, or infrastructure error |

## Common negative codes

| Code | Meaning |
| --- | --- |
| `-1` | JS-side validation or local wrapper error |
| `-2` | Method not registered / no handler |
| `-3` | Invalid parameters |
| `-5` | Invalid result |
| `-6` | Unauthorized access |
| `-7` | Operation cancelled |
| `-8` | Operation timeout |
| `-9` | Not found |
| `-997` | Frontend returned 404 |
| `-998` | Frontend method undefined |
| `-999` | Manual callback from business side |
| `-1000` | Unknown error |
| `-1001` | Network unreachable |
| `-1002` | Network timeout |
| `-1003` | Malformed response |

## Usage

```ts
methodCall(params, (res) => {
  if (res.code === 1) {
    // success
    return;
  }

  console.error(res.msg);
});
```

Use `res.code === 1` for success checks and `res.code !== 1` for failure checks.
Do not treat `0` as success.
