import hashlib
import os
from typing import Optional

from fastapi import FastAPI
from pydantic import BaseModel
from sqlalchemy import BigInteger, Boolean, Float, ForeignKey, Integer, String, UniqueConstraint, create_engine, inspect, text
from sqlalchemy.orm import DeclarativeBase, Mapped, Session, mapped_column, relationship

DATABASE_URL = os.environ.get("DATABASE_URL", "postgresql+psycopg2://coffio:coffio@db:5432/coffio")


class Base(DeclarativeBase):
    pass


class Coffee(Base):
    __tablename__ = "coffees"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(255), unique=True, nullable=False)


class Sieve(Base):
    __tablename__ = "sieves"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(255), unique=True, nullable=False)


class Drink(Base):
    __tablename__ = "drinks"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(255), unique=True, nullable=False)
    default_sieve_id: Mapped[Optional[int]] = mapped_column(ForeignKey("sieves.id"), nullable=True)
    default_coffee_id: Mapped[Optional[int]] = mapped_column(ForeignKey("coffees.id"), nullable=True)
    default_temperature: Mapped[float] = mapped_column(Float, default=93.0, nullable=False)
    default_coffee_weight: Mapped[float] = mapped_column(Float, default=18.0, nullable=False)
    default_target_yield: Mapped[float] = mapped_column(Float, default=36.0, nullable=False)
    default_grind_size: Mapped[float] = mapped_column(Float, default=2.0, nullable=False)
    default_desired_time: Mapped[float] = mapped_column(Float, default=25.0, nullable=False)
    default_tamper_pressure: Mapped[float] = mapped_column(Float, default=15.0, nullable=False)
    default_milk_volume: Mapped[float] = mapped_column(Float, default=0.0, nullable=False)
    is_visible: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)


class Brew(Base):
    __tablename__ = "brews"
    __table_args__ = (UniqueConstraint("signature", name="uq_brews_signature"),)

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    sync_key: Mapped[str] = mapped_column(String(64), unique=True, nullable=False)
    coffee_id: Mapped[int] = mapped_column(ForeignKey("coffees.id"), nullable=False)
    sieve_id: Mapped[int] = mapped_column(ForeignKey("sieves.id"), nullable=False)
    drink_id: Mapped[Optional[int]] = mapped_column(ForeignKey("drinks.id"), nullable=True)
    temperature: Mapped[float] = mapped_column(Float, nullable=False)
    coffee_weight: Mapped[float] = mapped_column(Float, nullable=False)
    target_yield: Mapped[float] = mapped_column(Float, nullable=False)
    actual_yield: Mapped[float] = mapped_column(Float, nullable=False)
    tamper_pressure: Mapped[float] = mapped_column(Float, nullable=False)
    milk_volume: Mapped[float] = mapped_column(Float, nullable=False)
    grind_size: Mapped[float] = mapped_column(Float, nullable=False)
    brew_time: Mapped[int] = mapped_column(Integer, nullable=False)
    timestamp: Mapped[int] = mapped_column(BigInteger, nullable=False)
    data_only: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    source: Mapped[str] = mapped_column(String(16), default="LOCAL", nullable=False)
    origin_device_id: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    signature: Mapped[str] = mapped_column(String(64), nullable=False)

    coffee: Mapped[Coffee] = relationship(Coffee)
    sieve: Mapped[Sieve] = relationship(Sieve)
    drink: Mapped[Optional[Drink]] = relationship(Drink)


class CoffeeDto(BaseModel):
    name: str


class SieveDto(BaseModel):
    name: str


class DrinkDto(BaseModel):
    name: str
    defaultSieveName: Optional[str] = None
    defaultCoffeeName: Optional[str] = None
    defaultTemperature: float = 93.0
    defaultCoffeeWeight: float = 18.0
    defaultTargetYield: float = 36.0
    defaultGrindSize: float = 2.0
    defaultDesiredTime: float = 25.0
    defaultTamperPressure: float = 15.0
    defaultMilkVolume: float = 0.0
    isVisible: bool = True


class BrewDto(BaseModel):
    syncKey: Optional[str] = None
    coffeeName: str
    sieveName: str
    drinkName: Optional[str] = None
    temperature: float
    coffeeWeight: float
    targetYield: float
    actualYield: float
    tamperPressure: float
    milkVolume: float
    grindSize: float
    brewTime: int
    timestamp: int
    dataOnly: bool = False
    source: Optional[str] = None
    originDeviceId: Optional[str] = None


class SyncRequest(BaseModel):
    deviceId: Optional[str] = None
    coffees: list[CoffeeDto] = []
    sieves: list[SieveDto] = []
    drinks: list[DrinkDto] = []
    brews: list[BrewDto] = []


class SyncResponse(BaseModel):
    coffees: list[CoffeeDto]
    sieves: list[SieveDto]
    drinks: list[DrinkDto]
    brews: list[BrewDto]


engine = create_engine(DATABASE_URL, future=True)


def migrate_schema() -> None:
    with engine.begin() as connection:
        inspector = inspect(connection)
        if "brews" in inspector.get_table_names():
            column_names = {column["name"] for column in inspector.get_columns("brews")}
            if "sync_key" not in column_names:
                connection.execute(text("ALTER TABLE brews ADD COLUMN sync_key VARCHAR(64)"))
                connection.execute(text("UPDATE brews SET sync_key = signature || '-' || id::text WHERE sync_key IS NULL"))
                connection.execute(text("ALTER TABLE brews ALTER COLUMN sync_key SET NOT NULL"))
                connection.execute(text("CREATE UNIQUE INDEX IF NOT EXISTS ux_brews_sync_key ON brews(sync_key)"))
            if "source" not in column_names:
                connection.execute(text("ALTER TABLE brews ADD COLUMN source VARCHAR(16) DEFAULT 'LOCAL'"))
                connection.execute(text("UPDATE brews SET source = 'LOCAL' WHERE source IS NULL"))
                connection.execute(text("ALTER TABLE brews ALTER COLUMN source SET NOT NULL"))
            if "origin_device_id" not in column_names:
                connection.execute(text("ALTER TABLE brews ADD COLUMN origin_device_id VARCHAR(255)"))


migrate_schema()
Base.metadata.create_all(engine)

app = FastAPI(title="Coffio Sync API", version="1.0.0")


def brew_signature(dto: BrewDto) -> str:
    raw = "|".join(
        [
            dto.coffeeName,
            dto.sieveName,
            dto.drinkName or "",
            f"{dto.temperature:.5f}",
            f"{dto.coffeeWeight:.5f}",
            f"{dto.targetYield:.5f}",
            f"{dto.actualYield:.5f}",
            f"{dto.tamperPressure:.5f}",
            f"{dto.milkVolume:.5f}",
            f"{dto.grindSize:.5f}",
            str(dto.brewTime),
            str(dto.timestamp),
            "1" if dto.dataOnly else "0",
        ]
    )
    return hashlib.sha256(raw.encode("utf-8")).hexdigest()


def brew_sync_key(dto: BrewDto) -> str:
    return dto.syncKey or brew_signature(dto)


def get_or_create_coffee(session: Session, name: str) -> Coffee:
    coffee = session.query(Coffee).filter(Coffee.name == name).one_or_none()
    if coffee is None:
        coffee = Coffee(name=name)
        session.add(coffee)
        session.flush()
    return coffee


def get_or_create_sieve(session: Session, name: str) -> Sieve:
    sieve = session.query(Sieve).filter(Sieve.name == name).one_or_none()
    if sieve is None:
        sieve = Sieve(name=name)
        session.add(sieve)
        session.flush()
    return sieve


@app.get("/api/v1/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/api/v1/sync", response_model=SyncResponse)
def sync(request: SyncRequest) -> SyncResponse:
    with Session(engine) as session:
        current_device_id = request.deviceId.strip() if request.deviceId else None
        for coffee_dto in request.coffees:
            if coffee_dto.name.strip():
                get_or_create_coffee(session, coffee_dto.name.strip())

        for sieve_dto in request.sieves:
            if sieve_dto.name.strip():
                get_or_create_sieve(session, sieve_dto.name.strip())

        for drink_dto in request.drinks:
            drink_name = drink_dto.name.strip()
            if not drink_name:
                continue

            default_coffee_id = None
            if drink_dto.defaultCoffeeName:
                default_coffee_id = get_or_create_coffee(session, drink_dto.defaultCoffeeName.strip()).id

            default_sieve_id = None
            if drink_dto.defaultSieveName:
                default_sieve_id = get_or_create_sieve(session, drink_dto.defaultSieveName.strip()).id

            existing_drink = session.query(Drink).filter(Drink.name == drink_name).one_or_none()
            if existing_drink is None:
                session.add(
                    Drink(
                        name=drink_name,
                        default_sieve_id=default_sieve_id,
                        default_coffee_id=default_coffee_id,
                        default_temperature=drink_dto.defaultTemperature,
                        default_coffee_weight=drink_dto.defaultCoffeeWeight,
                        default_target_yield=drink_dto.defaultTargetYield,
                        default_grind_size=drink_dto.defaultGrindSize,
                        default_desired_time=drink_dto.defaultDesiredTime,
                        default_tamper_pressure=drink_dto.defaultTamperPressure,
                        default_milk_volume=drink_dto.defaultMilkVolume,
                        is_visible=drink_dto.isVisible,
                    )
                )
            else:
                existing_drink.default_sieve_id = default_sieve_id
                existing_drink.default_coffee_id = default_coffee_id
                existing_drink.default_temperature = drink_dto.defaultTemperature
                existing_drink.default_coffee_weight = drink_dto.defaultCoffeeWeight
                existing_drink.default_target_yield = drink_dto.defaultTargetYield
                existing_drink.default_grind_size = drink_dto.defaultGrindSize
                existing_drink.default_desired_time = drink_dto.defaultDesiredTime
                existing_drink.default_tamper_pressure = drink_dto.defaultTamperPressure
                existing_drink.default_milk_volume = drink_dto.defaultMilkVolume
                existing_drink.is_visible = drink_dto.isVisible

        session.flush()

        coffee_by_name = {c.name: c.id for c in session.query(Coffee).all()}
        sieve_by_name = {s.name: s.id for s in session.query(Sieve).all()}
        drink_by_name = {d.name: d.id for d in session.query(Drink).all()}

        for brew_dto in request.brews:
            coffee_id = coffee_by_name.get(brew_dto.coffeeName)
            sieve_id = sieve_by_name.get(brew_dto.sieveName)
            if coffee_id is None or sieve_id is None:
                continue

            drink_id = drink_by_name.get(brew_dto.drinkName) if brew_dto.drinkName else None
            signature = brew_signature(brew_dto)
            sync_key = brew_sync_key(brew_dto)
            exists = session.query(Brew.id).filter((Brew.sync_key == sync_key) | (Brew.signature == signature)).first()
            if exists:
                continue

            session.add(
                Brew(
                    sync_key=sync_key,
                    coffee_id=coffee_id,
                    sieve_id=sieve_id,
                    drink_id=drink_id,
                    temperature=brew_dto.temperature,
                    coffee_weight=brew_dto.coffeeWeight,
                    target_yield=brew_dto.targetYield,
                    actual_yield=brew_dto.actualYield,
                    tamper_pressure=brew_dto.tamperPressure,
                    milk_volume=brew_dto.milkVolume,
                    grind_size=brew_dto.grindSize,
                    brew_time=brew_dto.brewTime,
                    timestamp=brew_dto.timestamp,
                    data_only=brew_dto.dataOnly,
                    source=(brew_dto.source or "LOCAL").upper(),
                    origin_device_id=brew_dto.originDeviceId or current_device_id,
                    signature=signature,
                )
            )

        session.commit()

        coffees = session.query(Coffee).order_by(Coffee.name.asc()).all()
        sieves = session.query(Sieve).order_by(Sieve.name.asc()).all()
        drinks = session.query(Drink).order_by(Drink.name.asc()).all()
        brews = session.query(Brew).order_by(Brew.timestamp.desc()).all()

        coffee_name_by_id = {c.id: c.name for c in coffees}
        sieve_name_by_id = {s.id: s.name for s in sieves}
        drink_name_by_id = {d.id: d.name for d in drinks}

        return SyncResponse(
            coffees=[CoffeeDto(name=c.name) for c in coffees],
            sieves=[SieveDto(name=s.name) for s in sieves],
            drinks=[
                DrinkDto(
                    name=d.name,
                    defaultSieveName=sieve_name_by_id.get(d.default_sieve_id),
                    defaultCoffeeName=coffee_name_by_id.get(d.default_coffee_id),
                    defaultTemperature=d.default_temperature,
                    defaultCoffeeWeight=d.default_coffee_weight,
                    defaultTargetYield=d.default_target_yield,
                    defaultGrindSize=d.default_grind_size,
                    defaultDesiredTime=d.default_desired_time,
                    defaultTamperPressure=d.default_tamper_pressure,
                    defaultMilkVolume=d.default_milk_volume,
                    isVisible=d.is_visible,
                )
                for d in drinks
            ],
            brews=[
                BrewDto(
                    syncKey=b.sync_key,
                    coffeeName=coffee_name_by_id[b.coffee_id],
                    sieveName=sieve_name_by_id[b.sieve_id],
                    drinkName=drink_name_by_id.get(b.drink_id),
                    temperature=b.temperature,
                    coffeeWeight=b.coffee_weight,
                    targetYield=b.target_yield,
                    actualYield=b.actual_yield,
                    tamperPressure=b.tamper_pressure,
                    milkVolume=b.milk_volume,
                    grindSize=b.grind_size,
                    brewTime=b.brew_time,
                    timestamp=b.timestamp,
                    dataOnly=b.data_only,
                    source=b.source,
                    originDeviceId=b.origin_device_id,
                )
                for b in brews
            ],
        )


@app.delete("/api/v1/data")
def delete_all_data() -> dict[str, str]:
    with Session(engine) as session:
        session.query(Brew).delete()
        session.query(Drink).delete()
        session.query(Sieve).delete()
        session.query(Coffee).delete()
        session.commit()
    return {"status": "deleted"}


@app.delete("/api/v1/brews/{sync_key}")
def delete_brew(sync_key: str) -> dict[str, str]:
    with Session(engine) as session:
        deleted = session.query(Brew).filter(Brew.sync_key == sync_key).delete()
        session.commit()
    if deleted == 0:
        return {"status": "not_found"}
    return {"status": "deleted"}


@app.put("/api/v1/brews/{sync_key}", response_model=dict[str, str])
def update_brew(sync_key: str, dto: BrewDto) -> dict[str, str]:
    with Session(engine) as session:
        brew = session.query(Brew).filter(Brew.sync_key == sync_key).one_or_none()
        if brew is None:
            return {"status": "not_found"}

        coffee = get_or_create_coffee(session, dto.coffeeName)
        sieve = get_or_create_sieve(session, dto.sieveName)
        drink_id = None
        if dto.drinkName:
            drink = session.query(Drink).filter(Drink.name == dto.drinkName).one_or_none()
            if drink:
                drink_id = drink.id

        brew.coffee_id = coffee.id
        brew.sieve_id = sieve.id
        brew.drink_id = drink_id
        brew.temperature = dto.temperature
        brew.coffee_weight = dto.coffeeWeight
        brew.target_yield = dto.targetYield
        brew.actual_yield = dto.actualYield
        brew.tamper_pressure = dto.tamperPressure
        brew.milk_volume = dto.milkVolume
        brew.grind_size = dto.grindSize
        brew.brew_time = dto.brewTime
        brew.timestamp = dto.timestamp
        brew.data_only = dto.dataOnly
        brew.source = (dto.source or brew.source or "LOCAL").upper()
        if dto.originDeviceId is not None:
            brew.origin_device_id = dto.originDeviceId
        brew.signature = brew_signature(dto)
        session.commit()
    return {"status": "updated"}
