import type { Lang } from './index';
import { plainText } from './inline';

export interface FaqEntry {
  q: string;
  /** May contain `inline code`; stripped for the FAQPage JSON-LD. */
  a: string;
}

/**
 * Six questions, phrased the way people actually search for them. Every answer is
 * traceable to README.md / plugin.xml / LICENSE.txt at github.com/fuuqiu/CommentNavigation.
 *
 * The "does it send my code anywhere" answer rests on a source check: grepping
 * src/main/kotlin for java.net, HttpClient, URL(, okhttp, ktor and Socket returns
 * nothing — the plugin only reads the open editor's text. Re-run that grep before
 * changing this answer.
 */
export const FAQ: Record<Lang, FaqEntry[]> = {
  en: [
    {
      q: 'Is Code Comment Navigator free?',
      a: 'Yes. It is released under the MIT License and is free for personal and commercial use — no purchase, no subscription, no activation key. You may modify it, redistribute it and sell it as long as the copyright and license notices are kept. The software is provided as is, with no fixed update schedule and no promised support response time.',
    },
    {
      q: 'Which IntelliJ IDEA versions does it support?',
      a: 'Compatibility covers IntelliJ IDEA 2025.1–2026.2 (builds 251–262.*). The plugin builds against the 2025.1 SDK and targets Java 21 bytecode. Building the plugin from source needs JDK 21.',
    },
    {
      q: 'Do I need the Database or HTTP Client plugins?',
      a: 'No. Parsing uses the platform Document API, so the outline has no dependency on the SQL/Database or HTTP Client language plugins. It reads `.sql`, `.http` and `.rest` files on its own.',
    },
    {
      q: 'Does it send my code anywhere?',
      a: 'No. The plugin contains no network code. It only reads the text of the editor you have open and builds the outline locally inside the IDE.',
    },
    {
      q: 'Why does `### Request name` show up as level 2?',
      a: 'Because it is the HTTP Client’s own request separator, and it is always mapped to a level-2 node so existing `.http` files keep working unchanged. Empty `###` lines are skipped. When you need an explicit third level, write `// ### Heading` instead.',
    },
    {
      q: 'Does it work in Scratch files?',
      a: 'Yes. Project files and Scratches are both supported — the ever-growing iteration scripts people keep in Scratches are the reason the outline exists. Open the scratch SQL or HTTP file and the panel follows the editor as usual.',
    },
  ],
  zh: [
    {
      q: 'Code Comment Navigator 是免费的吗？',
      a: '是。项目采用 MIT 许可证，个人和企业均可免费使用，包括商业用途，无需购买、订阅或激活码。允许修改、再分发及销售，须保留版权声明与许可声明。软件按现状提供，不承诺固定更新周期或支持响应时间。',
    },
    {
      q: '支持哪些版本的 IntelliJ IDEA？',
      a: '兼容 IntelliJ IDEA 2025.1–2026.2（251–262.*），基于 2025.1 SDK 构建并输出 Java 21 字节码。从源码构建插件需要 JDK 21。',
    },
    {
      q: '需要额外装 Database 或 HTTP Client 插件吗？',
      a: '不需要。解析使用平台 Document API，不依赖 SQL / Database 或 HTTP Client 语言插件，`.sql`、`.http`、`.rest` 文件都能自己读。',
    },
    {
      q: '它会把我的代码传到外部吗？',
      a: '不会。插件不含任何网络代码，只读取你当前打开的编辑器文本，并在 IDE 本地生成大纲。',
    },
    {
      q: '为什么 `### 请求名称` 是二级节点？',
      a: '因为它是 HTTP Client 原有的请求分隔符，为了让既有 `.http` 文件保持可用，它固定映射为二级节点；空的 `###` 会被跳过。需要明确的三级标题时，改写成 `// ### 标题`。',
    },
    {
      q: '在 Scratches 里能用吗？',
      a: '能。项目文件和 Scratches 都支持——Scratches 里越写越长的迭代脚本正是这个大纲存在的理由。打开 scratch 的 SQL 或 HTTP 文件，面板照常跟随编辑器。',
    },
  ],
};

/** Answer without backticks, for the FAQPage JSON-LD. */
export function plainAnswer(answer: string): string {
  return plainText(answer);
}
