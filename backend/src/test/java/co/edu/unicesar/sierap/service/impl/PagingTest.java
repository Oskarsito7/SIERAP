package co.edu.unicesar.sierap.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import co.edu.unicesar.sierap.dto.response.PagedResult;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Pruebas unitarias de {@link Paging}: orden aplicado a la paginación pedida, conversión
 * de página a resultado y normalización del texto de búsqueda (ADR-024).
 */
class PagingTest {

    private static final Sort ORDER = Sort.by("lastNames", "id");

    @Test
    void sortedKeepsThePageAndAppliesTheOrder() {
        Pageable result = Paging.sorted(PageRequest.of(2, 25), ORDER);

        assertThat(result.isPaged()).isTrue();
        assertThat(result.getPageNumber()).isEqualTo(2);
        assertThat(result.getPageSize()).isEqualTo(25);
        assertThat(result.getSort()).isEqualTo(ORDER);
    }

    @Test
    void sortedKeepsTheListUnpagedWhenNoPageWasRequested() {
        Pageable result = Paging.sorted(Pageable.unpaged(), ORDER);

        assertThat(result.isPaged()).isFalse();
        assertThat(result.getSort()).isEqualTo(ORDER);
    }

    @Test
    void toResultMapsTheItemsAndKeepsTheTotal() {
        PageImpl<Integer> page = new PageImpl<>(List.of(1, 2), PageRequest.of(0, 2), 7);

        PagedResult<String> result = Paging.toResult(page, number -> "n" + number);

        assertThat(result.items()).containsExactly("n1", "n2");
        assertThat(result.total()).isEqualTo(7);
    }

    @Test
    void likePatternRemovesAccentsAndCaseAndTrims() {
        assertThat(Paging.likePattern("  PÉREZ ")).isEqualTo("%perez%");
        assertThat(Paging.likePattern("Peña")).isEqualTo("%pena%");
        assertThat(Paging.likePattern("José")).isEqualTo("%jose%");
    }

    @Test
    void likePatternEscapesTheWildcards() {
        assertThat(Paging.likePattern("50%")).isEqualTo("%50!%%");
        assertThat(Paging.likePattern("a_b")).isEqualTo("%a!_b%");
        assertThat(Paging.likePattern("hola!")).isEqualTo("%hola!!%");
    }
}
