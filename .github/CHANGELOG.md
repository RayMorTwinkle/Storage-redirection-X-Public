- 支持 MediaProvider 重定向配置热重载，修改规则后无需重启应用即可让媒体查询过滤按新配置生效。
- 优化模块脚本与 Hook 配套逻辑，提升配置变更后的路径映射一致性。
- 【新增】每个应用可在"白名单"与"黑名单"两种重定向模式间切换：
  - 白名单：默认隔离全部存储，仅放行允许列表内的路径（原有行为）。
  - 黑名单：默认放行全部公共存储，仅隔离排除列表（黑名单）内的路径。
  - 规则支持通配符与 \! 排除前缀，模式可随时切换，旧配置默认白名单无损兼容。
- 【新增】系统写者/媒体进程（MediaProvider 等）也支持黑名单模式决策，并补充了相关 SELinux 放行规则。

<details>
<summary>English</summary>

- Support hot reload for MediaProvider redirect configuration so media-query filtering follows updated rules without restarting apps.
- Improve module scripts and Hook integration to keep path mapping consistent after configuration changes.
- [New] Each app can switch between "whitelist" and "blacklist" redirect modes:
  - Whitelist: isolate all storage by default, allow only paths in the allowed list (original behavior).
  - Blacklist: allow all public storage by default, isolate only paths in the excluded (blacklist) list.
  - Rules support wildcards and the \! exclusion prefix. Modes can be switched anytime; existing configs default to whitelist with no breaking change.
- [New] System-writer/media processes (MediaProvider, etc.) also honor blacklist decisions; added related SELinux allow rules.
</details>