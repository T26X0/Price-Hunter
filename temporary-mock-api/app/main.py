"""TEMPORARY mock API for Price Hunter frontend development."""

from datetime import datetime, timezone
from uuid import UUID, uuid4

from fastapi import FastAPI, HTTPException, status
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field


class ProductRequest(BaseModel):
    """Matches the current Spring Boot POST /api/products request body."""

    name: str = Field(min_length=1, max_length=200)
    sku: str = Field(min_length=1, max_length=100)
    description: str | None = Field(default=None, max_length=2000)


class ProductResponse(BaseModel):
    """Matches the current Java ProductResponse JSON format."""

    id: UUID
    name: str
    sku: str
    description: str | None
    createdAt: datetime


app = FastAPI(
    title="TEMPORARY Price Hunter Mock API",
    description="Temporary frontend development server. Replace with Spring Boot later.",
    version="0.1.0-temporary",
)

# Allows a Vite/React dev server to call this temporary API from a browser.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://127.0.0.1:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


def now() -> datetime:
    return datetime.now(timezone.utc)


products: list[ProductResponse] = [
    ProductResponse(
        id=UUID("5b23eb71-5ae9-4d98-bdc8-bd9e182d59dc"),
        name="iPhone 15",
        sku="IPHONE-15",
        description="Demo product for the temporary frontend API.",
        createdAt=now(),
    ),
    ProductResponse(
        id=UUID("0b1978ec-2455-4e21-8c80-f3eab18be289"),
        name="Samsung Galaxy S24",
        sku="GALAXY-S24",
        description="Demo product for the temporary frontend API.",
        createdAt=now(),
    ),
]


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "mode": "temporary-mock"}


@app.get("/api/products", response_model=list[ProductResponse])
def find_all_products() -> list[ProductResponse]:
    return sorted(products, key=lambda product: product.name.lower())


@app.post(
    "/api/products",
    response_model=ProductResponse,
    status_code=status.HTTP_201_CREATED,
)
def create_product(request: ProductRequest) -> ProductResponse:
    normalized_sku = request.sku.strip().upper()
    if any(product.sku == normalized_sku for product in products):
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=f"Product with SKU '{normalized_sku}' already exists.",
        )

    product = ProductResponse(
        id=uuid4(),
        name=request.name.strip(),
        sku=normalized_sku,
        description=request.description.strip() if request.description else None,
        createdAt=now(),
    )
    products.append(product)
    return product
