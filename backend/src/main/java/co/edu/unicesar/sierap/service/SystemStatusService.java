package co.edu.unicesar.sierap.service;

import co.edu.unicesar.sierap.dto.response.SystemStatusResponse;

/**
 * Reads the overall API status (version, active profile and database connection).
 */
public interface SystemStatusService {

    /**
     * Gets the current system status.
     *
     * @return status of the API and of the database connection
     */
    SystemStatusResponse getStatus();
}
