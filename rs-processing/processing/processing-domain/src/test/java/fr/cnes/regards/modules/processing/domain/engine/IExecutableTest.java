/* Copyright 2017-2022 CNES - CENTRE NATIONAL d'ETUDES SPATIALES
 *
 * This file is part of REGARDS.
 *
 * REGARDS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * REGARDS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with REGARDS. If not, see <http://www.gnu.org/licenses/>.
*/
package fr.cnes.regards.modules.processing.domain.engine;

import fr.cnes.regards.modules.processing.domain.execution.ExecutionContext;
import org.junit.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class IExecutableTest {

    ExecutionContext ctx = Mockito.mock(ExecutionContext.class);

    List<String> tokens = new ArrayList<>();

    IExecutable exec1 = context -> mkMono(tokens, context, "exec1");
    IExecutable exec2 = context -> mkMono(tokens, context, "exec2");

    IExecutable exec2e = context -> mkMono(tokens, context, "exec2")
            .flatMap(c -> {
                tokens.add("error");
                return Mono.error(new RuntimeException());
            });

    IExecutable exec3 = context -> mkMono(tokens, context, "exec3");

    @Test public void andThen1() {
        tokens.clear();

        IExecutable exec23 = exec2.andThen("exec3", exec3);
        IExecutable exec123 = exec1.andThen("exec23", exec23);

        exec123.execute(ctx).subscribe(x -> {}, t -> {});
        assertThat(tokens).containsExactly("exec1", "exec2", "exec3");
    }
    @Test public void andThen2() {            tokens.clear();

        IExecutable exec12 = exec1.andThen("exec2", exec2);
        IExecutable exec123 = exec12.andThen("exec3", exec3);

        exec123.execute(ctx).subscribe(x -> {}, t -> {});
        assertThat(tokens).containsExactly("exec1", "exec2", "exec3");
    }

    @Test public void onError1() {
        IExecutable exec12e3 = exec1
                .andThen(
                    "exec2",
                    exec2e // error occurs after exec2
                    .onError((c, t) -> { // immediate recovery
                        tokens.add("recover");
                        return Mono.just(c);
                    })
                    .andThen("exec3", exec3)
                );

        exec12e3.execute(ctx).subscribe(x -> {}, t -> {});
        assertThat(tokens).containsExactly("exec1", "exec2", "error", "recover", "exec3");
    }

    @Test public void onError2() {
        IExecutable exec12er3 = exec1
                .andThen("exec2e", exec2e) // error occurs after exec2
                .onError((c, t) -> { // recovery
                    tokens.add("recover");
                    return Mono.just(c);
                })
                .andThen("exec3", exec3);

        exec12er3.execute(ctx).subscribe(x -> {}, t -> {});
        assertThat(tokens).containsExactly("exec1", "exec2", "error", "recover", "exec3");
    }

    @Test public void onError3() {
        IExecutable exec12e3r = exec1
            .andThen("exec2e", exec2e) // error occurs after exec2
            .andThen("exec3", exec3)
            .onError((c, t) -> { // recovery after exec3
                tokens.add("recover");
                return Mono.just(c);
            });

        exec12e3r.execute(ctx).subscribe(x -> {}, t -> {});
        assertThat(tokens).containsExactly("exec1", "exec2", "error", "recover");
    }

    @Test public void interrupt1() {
        IExecutable exec12i = exec1.andThen("exec2", exec2.interrupt());
        IExecutable exec12i3 = exec12i.andThen("exec3", exec3);

        exec12i3.execute(ctx).subscribe(x -> {}, t -> {});
        assertThat(tokens).containsExactly("exec1", "exec2");
    }

    @Test public void interrupt2() {
        IExecutable exec12i = exec1.andThen("exec2", exec2).interrupt();
        IExecutable exec12i3 = exec12i.andThen("exec3", exec3);

        exec12i3.execute(ctx).subscribe(x -> {}, t -> {});
        assertThat(tokens).containsExactly("exec1", "exec2");
    }

    public Mono<ExecutionContext> mkMono(List<String> tokens, ExecutionContext context, String exec1) {
        return Mono.fromCallable(() -> {
            tokens.add(exec1);
            return context;
        });
    }
}