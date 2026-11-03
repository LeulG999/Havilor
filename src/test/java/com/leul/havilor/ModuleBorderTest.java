package com.leul.havilor;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

// The border rule (CLAUDE.md): a module calls another module's Service and uses its DTOs, never its
// entities or repository. PlaybookRepository is package-private, so the compiler already blocks it;
// this test also catches the entities, which have to be public for JPA.
class ModuleBorderTest {

    private static final Path ALERT_MODULE = Path.of("src/main/java/com/leul/havilor/alert");
    private static final List<String> FORBIDDEN = List.of(
            "import com.leul.havilor.playbook.Playbook;",
            "import com.leul.havilor.playbook.PlaybookStep;",
            "import com.leul.havilor.playbook.PlaybookRepository;",
            "import com.leul.havilor.playbook.*;");

    @Test
    void alertModuleUsesOnlyThePlaybookServiceAndItsDtos() throws IOException {
        try (Stream<Path> files = Files.walk(ALERT_MODULE)) {
            List<String> violations = files.filter(f -> f.toString().endsWith(".java"))
                    .flatMap(ModuleBorderTest::forbiddenImports)
                    .toList();

            assertThat(violations).isEmpty();
        }
    }

    private static Stream<String> forbiddenImports(Path file) {
        try {
            String source = Files.readString(file);
            return FORBIDDEN.stream().filter(source::contains).map(imp -> file.getFileName() + ": " + imp);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
