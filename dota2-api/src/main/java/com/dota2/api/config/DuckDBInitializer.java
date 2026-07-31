package com.dota2.api.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DuckDBInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        log.info("[DuckDB] Initializing database schemas...");

        try {
            // sys_user
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_user (" +
                    "id BIGINT PRIMARY KEY, " +
                    "user_name VARCHAR, " +
                    "pass_word VARCHAR, " +
                    "nick_name VARCHAR, " +
                    "email VARCHAR, " +
                    "phone VARCHAR, " +
                    "status INT DEFAULT 0, " +
                    "created_time TIMESTAMP, " +
                    "created_by BIGINT, " +
                    "updated_time TIMESTAMP, " +
                    "updated_by BIGINT, " +
                    "deleted INT DEFAULT 0" +
                    ")");

            // Seed default admin if missing or update password
            jdbcTemplate.execute("INSERT INTO sys_user (id, user_name, pass_word, nick_name, status, created_time, deleted) " +
                    "VALUES (1, 'admin', '0192023a7bbd73250516f069df18b500', '管理员', 0, CURRENT_TIMESTAMP, 0) " +
                    "ON CONFLICT (id) DO UPDATE SET pass_word = EXCLUDED.pass_word");
            log.info("[DuckDB] Default admin user ensured (admin / admin123)");

            // steam_account
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS steam_account (" +
                    "id BIGINT PRIMARY KEY, " +
                    "steam_id VARCHAR, " +
                    "nick_name VARCHAR, " +
                    "avatar VARCHAR, " +
                    "profile_url VARCHAR, " +
                    "last_fetch_time TIMESTAMP, " +
                    "status INT DEFAULT 0, " +
                    "created_time TIMESTAMP, " +
                    "created_by BIGINT, " +
                    "updated_time TIMESTAMP, " +
                    "updated_by BIGINT, " +
                    "deleted INT DEFAULT 0" +
                    ")");

            // match_main
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS match_main (" +
                    "id BIGINT PRIMARY KEY, " +
                    "match_id BIGINT, " +
                    "game_mode INT, " +
                    "lobby_type INT, " +
                    "radiant_win BOOLEAN, " +
                    "duration INT, " +
                    "start_time TIMESTAMP, " +
                    "radiant_score INT, " +
                    "dire_score INT, " +
                    "first_blood_time INT, " +
                    "cluster INT, " +
                    "patch INT, " +
                    "region INT, " +
                    "mvp_steam_id VARCHAR, " +
                    "fmvp_steam_id VARCHAR, " +
                    "created_time TIMESTAMP, " +
                    "deleted INT DEFAULT 0" +
                    ")");

            // match_player
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS match_player (" +
                    "id BIGINT PRIMARY KEY, " +
                    "match_id BIGINT, " +
                    "account_id BIGINT, " +
                    "player_slot INT, " +
                    "hero_id INT, " +
                    "kills INT, " +
                    "deaths INT, " +
                    "assists INT, " +
                    "leaver_status INT, " +
                    "gold INT, " +
                    "last_hits INT, " +
                    "denies INT, " +
                    "gold_per_min INT, " +
                    "xp_per_min INT, " +
                    "level INT, " +
                    "hero_damage INT, " +
                    "tower_damage INT, " +
                    "hero_healing INT, " +
                    "item_0 INT, item_1 INT, item_2 INT, item_3 INT, item_4 INT, item_5 INT, " +
                    "backpack_0 INT, backpack_1 INT, backpack_2 INT, " +
                    "item_neutral INT, " +
                    "kills_per_min DOUBLE, " +
                    "deaths_per_min DOUBLE, " +
                    "assists_per_min DOUBLE, " +
                    "kda DOUBLE, " +
                    "is_radiant BOOLEAN, " +
                    "win BOOLEAN, " +
                    "is_mvp BOOLEAN, " +
                    "is_fmvp BOOLEAN, " +
                    "created_time TIMESTAMP, " +
                    "deleted INT DEFAULT 0" +
                    ")");

            // match_detail
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS match_detail (" +
                    "id BIGINT PRIMARY KEY, " +
                    "match_id BIGINT, " +
                    "raw_json VARCHAR, " +
                    "created_time TIMESTAMP, " +
                    "deleted INT DEFAULT 0" +
                    ")");

            // hero_daily_stats
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS hero_daily_stats (" +
                    "id BIGINT PRIMARY KEY, " +
                    "stat_date VARCHAR, " +
                    "hero_id INT, " +
                    "matches_played INT, " +
                    "wins INT, " +
                    "created_time TIMESTAMP, " +
                    "deleted INT DEFAULT 0" +
                    ")");

            // asset_cache
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS asset_cache (" +
                    "id BIGINT PRIMARY KEY, " +
                    "asset_type VARCHAR, " +
                    "asset_key VARCHAR, " +
                    "mime_type VARCHAR, " +
                    "base64_data VARCHAR, " +
                    "created_time TIMESTAMP, " +
                    "deleted INT DEFAULT 0" +
                    ")");

            // sys_operation_log
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS sys_operation_log (" +
                    "id BIGINT PRIMARY KEY, " +
                    "user_id BIGINT, " +
                    "user_name VARCHAR, " +
                    "module VARCHAR, " +
                    "operation VARCHAR, " +
                    "method VARCHAR, " +
                    "params VARCHAR, " +
                    "time_taken BIGINT, " +
                    "ip VARCHAR, " +
                    "status INT, " +
                    "error_msg VARCHAR, " +
                    "created_time TIMESTAMP, " +
                    "deleted INT DEFAULT 0" +
                    ")");

            log.info("[DuckDB] All table schemas initialized successfully!");
        } catch (Exception e) {
            log.error("[DuckDB] Table initialization failed: {}", e.getMessage(), e);
        }
    }
}
