package com.ccut.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Shell 命令执行插件 — 允许 AI Agent 执行受限的 Shell 命令
 *
 * <p>安全措施：</p>
 * <ul>
 *     <li>工作目录限定在项目 sandbox 范围内</li>
 *     <li>命令超时 30 秒自动终止</li>
 *     <li>禁止危险命令（rm -rf、格式化、权限提升等）</li>
 * </ul>
 */
@Component
public class ShellExecutionPlugin implements ToolPlugin {

    private static final Logger logger = LoggerFactory.getLogger(ShellExecutionPlugin.class);
    private static final long TIMEOUT_SECONDS = 30;
    private static final int MAX_OUTPUT_LENGTH = 8000;

    private static final Set<String> BLOCKED_PATTERNS = Set.of(
            "rm -rf /", "rm -rf /*", "mkfs", "dd if=", ":(){ :|:& };:",
            "chmod 777 /", "chown root", "> /etc/", "shutdown", "reboot",
            "init 0", "init 6", "poweroff", "halt"
    );

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "ls", "cat", "head", "tail", "grep", "find", "wc", "sort",
            "uniq", "diff", "echo", "pwd", "whoami", "date", "uname",
            "ps", "top", "df", "du", "free", "uptime",
            "java", "javac", "mvn", "gradle", "npm", "node", "python3", "python",
            "git", "curl", "wget", "ping", "nslookup",
            "mkdir", "cp", "mv", "touch", "chmod", "tar", "zip", "unzip",
            "sed", "awk", "tr", "cut", "xargs", "tee",
            "pandoc", "libreoffice", "soffice"
    );

    @Value("${shell.sandbox-dir:#{systemProperties['user.dir']}}")
    private String sandboxDir;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public String getName() {
        return "execute_shell";
    }

    @Override
    public String getDescription() {
        return "在沙盒环境中执行 Shell 命令。可以用来运行程序、查看系统信息、管理文件等。" +
                "命令在工作目录下执行，超时30秒自动终止。" +
                "输入参数：command（要执行的命令），workDir（可选，相对于工作目录的子路径）。";
    }

    @Override
    public Class<?> getRequestType() {
        return ShellRequest.class;
    }

    @Override
    public Object execute(Object request) {
        ShellRequest req = (ShellRequest) request;
        String command = req.command();

        if (command == null || command.isBlank()) {
            return "{\"success\": false, \"error\": \"命令不能为空\"}";
        }

        // 安全检查：拦截危险命令
        String lowerCommand = command.toLowerCase().trim();
        for (String blocked : BLOCKED_PATTERNS) {
            if (lowerCommand.contains(blocked.toLowerCase())) {
                logger.warn("拦截危险命令: {}", command);
                return "{\"success\": false, \"error\": \"该命令被安全策略禁止\"}";
            }
        }

        // 验证基础命令是否在白名单中
        String baseCommand = extractBaseCommand(lowerCommand);
        if (!ALLOWED_COMMANDS.contains(baseCommand)) {
            logger.warn("命令不在白名单中: {}", baseCommand);
            return "{\"success\": false, \"error\": \"命令 '" + baseCommand + "' 不在允许列表中。允许的命令: " + ALLOWED_COMMANDS + "\"}";
        }

        // 确定工作目录
        File workDir = new File(sandboxDir);
        if (req.workDir() != null && !req.workDir().isBlank()) {
            workDir = new File(sandboxDir, req.workDir());
        }

        if (!workDir.exists() || !workDir.isDirectory()) {
            return "{\"success\": false, \"error\": \"工作目录不存在: " + workDir.getAbsolutePath() + "\"}";
        }

        logger.info("执行 Shell 命令: cmd='{}', workDir='{}'", command, workDir.getAbsolutePath());

        try {
            ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
            pb.directory(workDir);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return "{\"success\": false, \"error\": \"命令执行超时（" + TIMEOUT_SECONDS + "秒），已自动终止\"}";
            }

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.exitValue();

            // 截断过长输出
            if (output.length() > MAX_OUTPUT_LENGTH) {
                output = output.substring(0, MAX_OUTPUT_LENGTH) + "\n... (输出已截断，共 " + output.length() + " 字符)";
            }

            return "{\"success\": " + (exitCode == 0) + ", \"exitCode\": " + exitCode + ", \"output\": \"" +
                    escapeJson(output) + "\"}";

        } catch (Exception e) {
            logger.error("Shell 命令执行异常: {}", e.getMessage());
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String extractBaseCommand(String command) {
        String trimmed = command.trim().split("\\s+")[0];
        // 去掉路径前缀，只取命令名
        int lastSlash = trimmed.lastIndexOf('/');
        return lastSlash >= 0 ? trimmed.substring(lastSlash + 1) : trimmed;
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public record ShellRequest(
            String command,
            String workDir
    ) {}
}
